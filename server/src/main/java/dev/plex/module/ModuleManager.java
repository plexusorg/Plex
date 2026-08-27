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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
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

                    InputStreamReader internalModuleFile = new InputStreamReader(moduleDescriptor, StandardCharsets.UTF_8);
                    YamlConfiguration internalModuleConfig = YamlConfiguration.loadConfiguration(internalModuleFile);

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
                    if (apiCompatibility != plugin.getApi().compatibility().version())
                    {
                        throw new ModuleLoadException("Plex module " + name + " requires API compatibility " + apiCompatibility + ", but this Plex build provides API compatibility " + plugin.getApi().compatibility().version());
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
                    Class<? extends PlexModule> module = Class.forName(main, true, loader).asSubclass(PlexModule.class);

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
                catch (MalformedURLException | ReflectiveOperationException | ClassCastException e)
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
        this.modules.forEach(module ->
        {
            PlexLog.log("Loading module " + module.getPlexModuleFile().getName() + " with version " + module.getPlexModuleFile().getVersion());
            module.load();
            //            this.libraryLoader.createLoader(module, module.getPlexModuleFile());
        });
    }

    public void enableModules()
    {
        this.modules.forEach(module ->
        {
            PlexLog.log("Enabling module " + module.getPlexModuleFile().getName() + " with version " + module.getPlexModuleFile().getVersion());
            module.enable();
        });
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
            catch (RuntimeException ex)
            {
                PlexLog.error("Module " + module.getPlexModuleFile().getName() + " failed to disable: " + ex.getMessage());
            }
            finally
            {
                try
                {
                    module.getCommands().forEach(module::unregisterCommand);
                }
                finally
                {
                    try
                    {
                        module.getListeners().forEach(module::unregisterListener);
                    }
                    finally
                    {
                        module.cancelTasks();
                    }
                }
            }
        });
    }

    public void unloadModules()
    {
        this.disableModules();
        this.modules.forEach(module ->
        {
            try
            {
                ((URLClassLoader) module.getClass().getClassLoader()).close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        });
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
}
