package dev.plex.module;

import org.bukkit.Bukkit;


import com.google.common.collect.Lists;
import dev.plex.Plex;
import dev.plex.api.module.ModulesApi;
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
import java.util.Collection;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.configuration.file.YamlConfiguration;

public class ModuleManager implements ModulesApi
{
    private final Plex plugin;
    private final List<LoadedModule> loadedModules = Lists.newArrayList();
    private volatile List<LoadedModule> loadedSnapshot = List.of();
    private CompletableFuture<Void> lifecycleTail = CompletableFuture.completedFuture(null);
    private volatile CompletableFuture<Void> activeClosures = CompletableFuture.completedFuture(null);
    private boolean shuttingDown;

    public ModuleManager(Plex plugin)
    {
        this.plugin = plugin;
    }

    public void load()
    {
        discoverModules();
        loadModules();
        publishSnapshot();
    }

    private void discoverModules()
    {
        this.loadedModules.clear();
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
            File dataFolder = new File(plugin.getModulesFolder(), moduleFile.getName());
            dataFolder.mkdirs();
            module.setPlexModuleFile(moduleFile);
            module.setDataFolder(dataFolder);
            Logger logger = LogManager.getLogger(moduleFile.getName());
            module.setLogger(logger);
            loadedModules.add(new LoadedModule(module, plugin.getApi(), moduleFile, file, dataFolder, loader));
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
                : config.getConfigurationSection("repositories").getKeys(false).stream()
                        .map(id -> config.getConfigurationSection("repositories").getString(id, ""))
                        .filter(repository -> !repository.isBlank()).toList();
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
        if (loadedModules.stream().anyMatch(loaded -> loaded.descriptor().getName().equalsIgnoreCase(moduleFile.getName())))
        {
            throw new ModuleLoadException("Plex module name is already in use: " + moduleFile.getName());
        }
        String storagePrefix = ModuleNames.prefix(moduleFile.getName());
        if (loadedModules.stream().anyMatch(loaded -> ModuleNames.prefix(loaded.module()).equals(storagePrefix)))
        {
            throw new ModuleLoadException("Plex module storage name is already in use: " + storagePrefix);
        }
    }

    private void loadModules()
    {
        Iterator<LoadedModule> iterator = loadedModules.iterator();
        while (iterator.hasNext())
        {
            LoadedModule loaded = iterator.next();
            PlexModule module = loaded.module();
            PlexLog.log("Loading module " + loaded.descriptor().getName() + " with version " + loaded.descriptor().getVersion());
            try
            {
                module.load();
            }
            catch (RuntimeException | LinkageError ex)
            {
                PlexLog.error("Module " + loaded.descriptor().getName() + " failed to load", ex);
                loaded.cleanupContributions();
                loaded.close();
                iterator.remove();
            }
        }
    }

    public void enableModules()
    {
        Iterator<LoadedModule> iterator = loadedModules.iterator();
        while (iterator.hasNext())
        {
            LoadedModule loaded = iterator.next();
            PlexModule module = loaded.module();
            PlexLog.log("Enabling module " + loaded.descriptor().getName() + " with version " + loaded.descriptor().getVersion());
            try
            {
                module.enable();
            }
            catch (RuntimeException | LinkageError ex)
            {
                PlexLog.error("Module " + loaded.descriptor().getName() + " failed to enable", ex);
                try
                {
                    module.disable();
                }
                catch (RuntimeException | LinkageError cleanupFailure)
                {
                    PlexLog.error("Module " + loaded.descriptor().getName()
                            + " also failed rollback disable", cleanupFailure);
                }
                loaded.cleanupContributions();
                loaded.close();
                iterator.remove();
            }
        }
        publishSnapshot();
    }

    private void disableModules()
    {
        this.loadedModules.forEach(loaded ->
        {
            PlexModule module = loaded.module();
            PlexLog.log("Disabling module " + loaded.descriptor().getName() + " with version " + loaded.descriptor().getVersion());
            try
            {
                module.disable();
            }
            catch (RuntimeException | LinkageError ex)
            {
                PlexLog.error("Module " + loaded.descriptor().getName() + " failed to disable", ex);
            }
            finally
            {
                loaded.cleanupContributions();
            }
        });
    }

    public CompletableFuture<Void> unloadModules()
    {
        return queueLifecycle(this::unloadModulesOnGlobal);
    }

    /**
     * Begins module shutdown while the server itself is already on the global owner.
     */
    public CompletableFuture<Void> unloadModulesDuringServerShutdown()
    {
        CompletableFuture<Void> previousClosures;
        synchronized (this)
        {
            shuttingDown = true;
            previousClosures = activeClosures;
        }
        return CompletableFuture.allOf(previousClosures, beginUnloadModules());
    }

    private CompletableFuture<Void> beginUnloadModules()
    {
        this.disableModules();
        CompletableFuture<?>[] closures = this.loadedModules.stream()
                .map(LoadedModule::close).toArray(CompletableFuture[]::new);
        this.loadedModules.clear();
        publishSnapshot();
        activeClosures = CompletableFuture.allOf(closures);
        return activeClosures;
    }

    public CompletableFuture<Void> reloadModules()
    {
        return queueLifecycle(() -> unloadModulesOnGlobal()
                .thenCompose(ignored -> reloadAfterLifecycleOperation()));
    }

    private CompletableFuture<Void> unloadModulesOnGlobal()
    {
        return onGlobalResult(this::beginUnloadModules).thenCompose(closures -> closures);
    }

    private void reloadFromDisk()
    {
        discoverModules();
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
    public CompletableFuture<UninstallResult> uninstallModule(String name, boolean removeData)
    {
        return queueLifecycle(() -> onGlobalResult(() -> prepareUninstall(name))
                .thenCompose(target ->
                {
                    if (target == null)
                    {
                        return CompletableFuture.completedFuture(UninstallResult.NOT_FOUND);
                    }
                    return target.closure().thenApplyAsync(ignored ->
                            {
                                boolean deleted = target.jar().delete();
                                if (deleted && removeData && target.dataFolder().isDirectory())
                                {
                                    deleteRecursively(target.dataFolder());
                                }
                                return deleted ? UninstallResult.REMOVED : UninstallResult.FAILED;
                            }, plugin.getIoExecutor())
                            .thenCompose(result -> reloadAfterLifecycleOperation().thenApply(ignored -> result));
                }));
    }

    private UninstallTarget prepareUninstall(String name)
    {
        LoadedModule target = loadedModules.stream()
                .filter(module -> module.descriptor().getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
        return target == null ? null
                : new UninstallTarget(target.jar(), target.dataFolder(), beginUnloadModules());
    }

    private synchronized <T> CompletableFuture<T> queueLifecycle(Supplier<CompletableFuture<T>> operation)
    {
        if (shuttingDown)
        {
            return CompletableFuture.failedFuture(new IllegalStateException("Module lifecycle is shutting down"));
        }
        CompletableFuture<T> result = lifecycleTail.handle((ignored, failure) -> null)
                .thenCompose(ignored ->
                {
                    synchronized (this)
                    {
                        if (shuttingDown)
                        {
                            return CompletableFuture.failedFuture(
                                    new IllegalStateException("Module lifecycle is shutting down"));
                        }
                        return operation.get();
                    }
                });
        lifecycleTail = result.handle((ignored, failure) -> null);
        return result;
    }

    private CompletableFuture<Void> reloadAfterLifecycleOperation()
    {
        synchronized (this)
        {
            if (shuttingDown)
            {
                return CompletableFuture.completedFuture(null);
            }
        }
        return onGlobal(() ->
        {
            synchronized (this)
            {
                if (shuttingDown)
                {
                    return;
                }
            }
            reloadFromDisk();
        });
    }

    private CompletableFuture<Void> onGlobal(Runnable action)
    {
        CompletableFuture<Void> completion = new CompletableFuture<>();
        Bukkit.getGlobalRegionScheduler().execute(plugin, () ->
        {
            try
            {
                action.run();
                completion.complete(null);
            }
            catch (RuntimeException | LinkageError failure)
            {
                completion.completeExceptionally(failure);
            }
        });
        return completion;
    }

    private <T> CompletableFuture<T> onGlobalResult(Supplier<T> action)
    {
        CompletableFuture<T> completion = new CompletableFuture<>();
        Bukkit.getGlobalRegionScheduler().execute(plugin, () ->
        {
            try
            {
                completion.complete(action.get());
            }
            catch (RuntimeException | LinkageError failure)
            {
                completion.completeExceptionally(failure);
            }
        });
        return completion;
    }

    private record UninstallTarget(File jar, File dataFolder, CompletableFuture<Void> closure)
    {
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

    public List<PlexModule> getModules()
    {
        return loadedSnapshot.stream().map(LoadedModule::module).toList();
    }

    @Override
    public Collection<PlexModuleFile> loadedModules()
    {
        return loadedSnapshot.stream().map(LoadedModule::descriptor).toList();
    }

    @Override
    public Optional<PlexModuleFile> module(String name)
    {
        String normalizedName = Objects.requireNonNull(name, "name").toLowerCase(Locale.ROOT);
        return loadedSnapshot.stream().map(LoadedModule::descriptor)
                .filter(module -> module.getName().toLowerCase(Locale.ROOT).equals(normalizedName))
                .findFirst();
    }

    public File moduleJar(PlexModule module)
    {
        return loadedSnapshot.stream().filter(loaded -> loaded.module() == module)
                .findFirst().orElseThrow(() -> new IllegalArgumentException("Module is not loaded")).jar();
    }

    private void publishSnapshot()
    {
        loadedSnapshot = List.copyOf(loadedModules);
    }
}
