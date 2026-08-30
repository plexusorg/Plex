package dev.plex.module;

import com.google.common.collect.Lists;
import dev.plex.Plex;
import dev.plex.module.exception.ModuleLoadException;
import dev.plex.storage.module.ModuleNames;
import dev.plex.util.PlexLog;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.bukkit.configuration.file.YamlConfiguration;

@Getter
public class ModuleManager
{
    private final Plex plugin;
    private final List<PlexModule> modules = Lists.newArrayList();

    public ModuleManager(Plex plugin)
    {
        this.plugin = plugin;
    }

    public void loadAllModules()
    {
        this.modules.clear();
        File[] moduleFiles = plugin.getModulesFolder().listFiles();
        if (moduleFiles == null)
        {
            PlexLog.warn("Unable to read modules folder " + plugin.getModulesFolder().getAbsolutePath());
            return;
        }

        PlexLog.debug(String.valueOf(moduleFiles.length));
        Arrays.stream(moduleFiles).forEach(file ->
        {
            if (file.getName().endsWith(".jar"))
            {
                URLClassLoader loader = null;
                try
                {
                    loader = new URLClassLoader(
                            new URL[]{file.toURI().toURL()},
                            Plex.class.getClassLoader()
                    );

                    InputStream moduleDescriptor = loader.getResourceAsStream("module.yml");
                    if (moduleDescriptor == null)
                    {
                        throw new ModuleLoadException("Plex module " + file.getName() + " does not contain module.yml");
                    }

                    YamlConfiguration internalModuleConfig;
                    try (moduleDescriptor;
                         InputStreamReader internalModuleFile = new InputStreamReader(moduleDescriptor, StandardCharsets.UTF_8))
                    {
                        internalModuleConfig = YamlConfiguration.loadConfiguration(internalModuleFile);
                    }

                    String name = internalModuleConfig.getString("name");
                    if (name == null || !name.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,63}"))
                    {
                        throw new ModuleLoadException("Plex module name is invalid");
                    }
                    if (modules.stream().anyMatch(loaded -> loaded.getPlexModuleFile().getName().equalsIgnoreCase(name)))
                    {
                        throw new ModuleLoadException("Plex module name is already in use: " + name);
                    }
                    String storagePrefix = ModuleNames.prefix(name);
                    if (modules.stream().anyMatch(loaded -> ModuleNames.prefix(loaded).equals(storagePrefix)))
                    {
                        throw new ModuleLoadException("Plex module storage name is already in use: " + storagePrefix);
                    }

                    String main = internalModuleConfig.getString("main");
                    if (main == null)
                    {
                        throw new ModuleLoadException("Plex module main class can't be null!");
                    }

                    String description = internalModuleConfig.getString("description", "A Plex module");
                    String version = internalModuleConfig.getString("version", "1.0");
                    if (!internalModuleConfig.isInt("apiCompatibility"))
                    {
                        throw new ModuleLoadException("Plex module " + name + " must declare an integer apiCompatibility in module.yml");
                    }

                    int apiCompatibility = internalModuleConfig.getInt("apiCompatibility");
                    if (apiCompatibility != plugin.getApi().apiCompatibilityVersion())
                    {
                        throw new ModuleLoadException("Plex module " + name + " requires API compatibility " + apiCompatibility + ", but this Plex build provides API compatibility " + plugin.getApi().apiCompatibilityVersion());
                    }

                    List<String> libraries = internalModuleConfig.getStringList("libraries");
                    List<String> repositories = internalModuleConfig.getConfigurationSection("repositories") == null
                            ? List.of()
                            : internalModuleConfig.getConfigurationSection("repositories").getKeys(false).stream()
                                    .map(id -> internalModuleConfig.getConfigurationSection("repositories").getString(id, ""))
                                    .filter(repository -> !repository.isBlank())
                                    .toList();
                    boolean updaterEnabled = internalModuleConfig.getBoolean("updater.enabled", true);
                    List<String> updateUrls = new ArrayList<>();
                    String updateUrl = internalModuleConfig.getString("updater.url", "");
                    if (!updateUrl.isBlank())
                    {
                        updateUrls.add(updateUrl);
                    }
                    updateUrls.addAll(internalModuleConfig.getStringList("updater.urls").stream()
                            .filter(url -> !url.isBlank())
                            .toList());

                    PlexModuleFile plexModuleFile = new PlexModuleFile(name, main, description, version,
                            apiCompatibility, libraries, repositories, updaterEnabled, updateUrls);
                    Class<? extends PlexModule> module = Class.forName(main, false, loader).asSubclass(PlexModule.class);
                    if (module.getClassLoader() != loader)
                    {
                        throw new ModuleLoadException("Plex module main class must be defined by " + file.getName());
                    }

                    PlexModule plexModule = module.getConstructor().newInstance();
                    plexModule.setApi(plugin.getApi());
                    plexModule.setPlexModuleFile(plexModuleFile);
                    plexModule.setModuleJar(file);

                    plexModule.setDataFolder(new File(plugin.getModulesFolder() + File.separator + plexModuleFile.getName()));
                    if (!plexModule.getDataFolder().exists())
                    {
                        plexModule.getDataFolder().mkdir();
                    }

                    plexModule.setLogger(LogManager.getLogger(plexModuleFile.getName()));
                    modules.add(plexModule);
                    loader = null;
                }
                catch (IOException | ReflectiveOperationException | ClassCastException e)
                {
                    PlexLog.warn("Skipping module " + file.getName() + ": " + e.getMessage());
                }
                catch (ModuleLoadException e)
                {
                    PlexLog.warn("Skipping module " + file.getName() + ": " + e.getMessage());
                }
                finally
                {
                    if (loader != null)
                    {
                        try
                        {
                            loader.close();
                        }
                        catch (IOException ex)
                        {
                            PlexLog.warn("Could not close module file " + file.getName() + ": " + ex.getMessage());
                        }
                    }
                }
            }
        });
    }

    public void loadModules()
    {
        Iterator<PlexModule> iterator = modules.iterator();
        while (iterator.hasNext())
        {
            PlexModule module = iterator.next();
            PlexLog.log("Loading module " + module.getPlexModuleFile().getName() + " with version " + module.getPlexModuleFile().getVersion());
            try
            {
                module.load();
            }
            catch (RuntimeException | LinkageError ex)
            {
                PlexLog.error("Module " + module.getPlexModuleFile().getName() + " failed to load: " + ex.getMessage());
                cleanupContributions(module);
                closeClassLoader(module);
                iterator.remove();
            }
        }
    }

    public void enableModules()
    {
        Iterator<PlexModule> iterator = modules.iterator();
        while (iterator.hasNext())
        {
            PlexModule module = iterator.next();
            PlexLog.log("Enabling module " + module.getPlexModuleFile().getName() + " with version " + module.getPlexModuleFile().getVersion());
            try
            {
                module.enable();
            }
            catch (RuntimeException | LinkageError ex)
            {
                PlexLog.error("Module " + module.getPlexModuleFile().getName() + " failed to enable: " + ex.getMessage());
                try
                {
                    module.disable();
                }
                catch (RuntimeException | LinkageError cleanupFailure)
                {
                    PlexLog.error("Module " + module.getPlexModuleFile().getName()
                            + " also failed rollback disable: " + cleanupFailure.getMessage());
                }
                cleanupContributions(module);
                closeClassLoader(module);
                iterator.remove();
            }
        }
    }

    public void disableModules()
    {
        this.modules.forEach(module ->
        {
            PlexLog.log("Disabling module " + module.getPlexModuleFile().getName() + " with version " + module.getPlexModuleFile().getVersion());
            try
            {
                module.disable();
            }
            catch (RuntimeException | LinkageError ex)
            {
                PlexLog.error("Module " + module.getPlexModuleFile().getName() + " failed to disable: " + ex.getMessage());
            }
            finally
            {
                cleanupContributions(module);
            }
        });
    }

    public void unloadModules()
    {
        this.disableModules();
        this.modules.forEach(this::closeClassLoader);
        this.modules.clear();
    }

    public void reloadModules()
    {
        unloadModules();
        reloadFromDisk();
    }

    private void reloadFromDisk()
    {
        loadAllModules();
        loadModules();
        enableModules();
        if (plugin.getCommandHandler() != null && plugin.getCommandHandler().requiresLifecycleReload())
        {
            PlexLog.warn("Module command changes were staged after Paper's Brigadier command lifecycle. Restart the server for the live command dispatcher to match the loaded modules.");
        }
    }

    /**
     * Outcome of an uninstall request.
     */
    public enum UninstallResult
    {
        NOT_FOUND,
        REMOVED,
        FAILED
    }

    /**
     * Uninstalls a loaded module by name: deletes its JAR and, optionally, its data
     * folder, then reloads the remaining modules.
     *
     * @param name module name as declared in the module's module.yml
     * @param removeData whether to also delete the module's data folder
     * @return the outcome of the uninstall request
     */
    public UninstallResult uninstallModule(String name, boolean removeData)
    {
        PlexModule target = modules.stream()
                .filter(module -> module.getPlexModuleFile().getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
        if (target == null)
        {
            return UninstallResult.NOT_FOUND;
        }

        File moduleJar = target.getModuleJar();
        File dataFolder = target.getDataFolder();

        unloadModules();

        boolean deleted = moduleJar.delete();
        if (deleted && removeData && dataFolder.isDirectory())
        {
            deleteRecursively(dataFolder);
        }

        reloadFromDisk();

        return deleted ? UninstallResult.REMOVED : UninstallResult.FAILED;
    }

    private void deleteRecursively(File file)
    {
        File[] children = file.listFiles();
        if (children != null)
        {
            for (File child : children)
            {
                deleteRecursively(child);
            }
        }
        if (!file.delete())
        {
            PlexLog.warn("Unable to delete " + file.getAbsolutePath());
        }
    }

    private void cleanupContributions(PlexModule module)
    {
        String name = module.getPlexModuleFile().getName();
        try
        {
            module.getCommands().forEach(module::unregisterCommand);
            module.getListeners().forEach(module::unregisterListener);
        }
        catch (RuntimeException | LinkageError ex)
        {
            PlexLog.error("Could not unregister all contributions from module " + name + ": " + ex.getMessage());
        }
        try
        {
            module.cancelTasks();
        }
        catch (RuntimeException | LinkageError ex)
        {
            PlexLog.error("Could not cancel tasks for module " + name + ": " + ex.getMessage());
        }
    }

    private void closeClassLoader(PlexModule module)
    {
        ClassLoader classLoader = module.getClass().getClassLoader();
        if (!(classLoader instanceof URLClassLoader loader))
        {
            return;
        }
        try
        {
            loader.close();
        }
        catch (IOException ex)
        {
            PlexLog.error("Could not close module " + module.getPlexModuleFile().getName() + " classloader: " + ex.getMessage());
        }
    }
}
