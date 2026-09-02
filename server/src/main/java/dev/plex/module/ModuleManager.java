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
        for (File file : moduleFiles)
        {
            if (!file.getName().endsWith(".jar"))
            {
                continue;
            }
            loadModule(file);
        }
    }

    private void loadModule(File file)
    {
        URLClassLoader loader = null;
        try
        {
            loader = new URLClassLoader(new URL[]{file.toURI().toURL()}, Plex.class.getClassLoader());
            PlexModuleFile moduleFile = readModuleFile(file, loader);
            rejectDuplicate(moduleFile);
            Class<? extends PlexModule> type = Class.forName(moduleFile.getMain(), false, loader).asSubclass(PlexModule.class);
            if (type.getClassLoader() != loader)
            {
                throw new ModuleLoadException("Plex module main class must be defined by " + file.getName());
            }
            PlexModule module = type.getConstructor().newInstance();
            module.setApi(plugin.getApi());
            module.setPlexModuleFile(moduleFile);
            module.setModuleJar(file);
            module.setDataFolder(new File(plugin.getModulesFolder(), moduleFile.getName()));
            module.getDataFolder().mkdirs();
            module.setLogger(LogManager.getLogger(moduleFile.getName()));
            modules.add(module);
            loader = null;
        }
        catch (IOException | ReflectiveOperationException | ClassCastException | ModuleLoadException e)
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

    private PlexModuleFile readModuleFile(File file, URLClassLoader loader) throws IOException, ModuleLoadException
    {
        InputStream descriptor = loader.getResourceAsStream("module.yml");
        if (descriptor == null)
        {
            throw new ModuleLoadException("Plex module " + file.getName() + " does not contain module.yml");
        }
        YamlConfiguration config;
        try (descriptor; InputStreamReader reader = new InputStreamReader(descriptor, StandardCharsets.UTF_8))
        {
            config = YamlConfiguration.loadConfiguration(reader);
        }
        String name = config.getString("name");
        if (!config.isInt("apiCompatibility"))
        {
            throw new ModuleLoadException("Plex module " + name + " must declare an integer apiCompatibility in module.yml");
        }
        int compatibility = config.getInt("apiCompatibility");
        if (compatibility != plugin.getApi().apiCompatibilityVersion())
        {
            throw new ModuleLoadException("Plex module " + name + " requires API compatibility " + compatibility + ", but this Plex build provides API compatibility " + plugin.getApi().apiCompatibilityVersion());
        }
        List<String> repositories = config.getConfigurationSection("repositories") == null ? List.of()
                : config.getConfigurationSection("repositories").getValues(false).values().stream()
                        .map(String::valueOf).filter(repository -> !repository.isBlank()).toList();
        List<String> updateUrls = new ArrayList<>(config.getStringList("updater.urls").stream()
                .filter(url -> !url.isBlank()).toList());
        String updateUrl = config.getString("updater.url", "");
        if (!updateUrl.isBlank())
        {
            updateUrls.addFirst(updateUrl);
        }
        try
        {
            return new PlexModuleFile(name, config.getString("main"), config.getString("description", "A Plex module"),
                    config.getString("version", "1.0"), compatibility, config.getStringList("libraries"), repositories,
                    config.getBoolean("updater.enabled", true), updateUrls);
        }
        catch (IllegalArgumentException | NullPointerException e)
        {
            throw new ModuleLoadException("Invalid module.yml in " + file.getName() + ": " + e.getMessage());
        }
    }

    private void rejectDuplicate(PlexModuleFile moduleFile) throws ModuleLoadException
    {
        if (modules.stream().anyMatch(loaded -> loaded.getPlexModuleFile().getName().equalsIgnoreCase(moduleFile.getName())))
        {
            throw new ModuleLoadException("Plex module name is already in use: " + moduleFile.getName());
        }
        String storagePrefix = ModuleNames.prefix(moduleFile.getName());
        if (modules.stream().anyMatch(loaded -> ModuleNames.prefix(loaded).equals(storagePrefix)))
        {
            throw new ModuleLoadException("Plex module storage name is already in use: " + storagePrefix);
        }
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
                PlexLog.error("Module " + module.getPlexModuleFile().getName() + " failed to load", ex);
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
                PlexLog.error("Module " + module.getPlexModuleFile().getName() + " failed to enable", ex);
                try
                {
                    module.disable();
                }
                catch (RuntimeException | LinkageError cleanupFailure)
                {
                    PlexLog.error("Module " + module.getPlexModuleFile().getName()
                            + " also failed rollback disable", cleanupFailure);
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
                PlexLog.error("Module " + module.getPlexModuleFile().getName() + " failed to disable", ex);
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
            PlexLog.error("Could not unregister all contributions from module " + name, ex);
        }
        try
        {
            module.cancelTasks();
        }
        catch (RuntimeException | LinkageError ex)
        {
            PlexLog.error("Could not cancel tasks for module " + name, ex);
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
            PlexLog.error("Could not close module " + module.getPlexModuleFile().getName() + " classloader", ex);
        }
    }
}
