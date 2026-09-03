package dev.plex.module;

import dev.plex.api.PlexApi;
import dev.plex.Plex;
import dev.plex.command.PlexCommand;
import dev.plex.util.PlexLog;
import java.io.File;
import java.io.IOException;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.Nullable;

final class LoadedModule implements ModuleLifecycle
{
    private final PlexModule module;
    private final PlexApi api;
    private final Set<ScheduledTask> tasks = ConcurrentHashMap.newKeySet();
    private volatile boolean acceptingTasks = true;
    private final PlexModuleFile descriptor;
    private final File jar;
    private final File dataFolder;
    private final URLClassLoader classLoader;
    private final Set<PlexCommand> commands = new LinkedHashSet<>();
    private final Set<Listener> listeners = Collections.newSetFromMap(new IdentityHashMap<>());
    private CompletableFuture<Void> shutdownCompletion = CompletableFuture.completedFuture(null);

    LoadedModule(PlexModule module, PlexApi api, PlexModuleFile descriptor, File jar, File dataFolder,
                 URLClassLoader classLoader)
    {
        this.module = module;
        this.api = api;
        this.descriptor = descriptor;
        this.jar = jar;
        this.dataFolder = dataFolder;
        this.classLoader = classLoader;
        module.setLifecycle(this);
    }

    PlexModule module()
    {
        return module;
    }

    PlexModuleFile descriptor()
    {
        return descriptor;
    }

    File jar()
    {
        return jar;
    }

    File dataFolder()
    {
        return dataFolder;
    }

    @Override
    public PlexApi api()
    {
        return api;
    }

    @Override
    public Plugin plugin()
    {
        return Plex.get();
    }

    @Override
    public <T extends ScheduledTask> @Nullable T ownTask(@Nullable T task)
    {
        if (task == null) return null;
        if (!acceptingTasks)
        {
            task.cancel();
            throw new IllegalStateException("Module is unloading");
        }
        tasks.removeIf(LoadedModule::finished);
        tasks.add(task);
        if (!acceptingTasks && tasks.remove(task)) task.cancel();
        return task;
    }

    private static boolean finished(ScheduledTask task)
    {
        ScheduledTask.ExecutionState state = task.getExecutionState();
        return state == ScheduledTask.ExecutionState.FINISHED
                || state == ScheduledTask.ExecutionState.CANCELLED
                || state == ScheduledTask.ExecutionState.CANCELLED_RUNNING;
    }

    @Override
    public void kickPlayerOnShutdown(Player player, Component reason)
    {
        player.getScheduler().execute(Plex.get(), () -> player.kick(reason), null, 0L);
    }

    @Override
    public void completeShutdownBeforeClose(CompletableFuture<Void> completion)
    {
        shutdownCompletion = CompletableFuture.allOf(shutdownCompletion, Objects.requireNonNull(completion, "completion"));
    }

    @Override
    public void registerCommand(PlexCommand command)
    {
        Objects.requireNonNull(command, "command");
        if (!commands.add(command))
        {
            throw new IllegalStateException("Command is already registered");
        }
        boolean registrationStarted = false;
        try
        {
            command.bindApi(api);
            command.bindModule(module);
            registrationStarted = true;
            api.commands().register(command);
        }
        catch (RuntimeException | LinkageError failure)
        {
            if (registrationStarted)
            {
                try
                {
                    api.commands().unregister(command);
                }
                catch (RuntimeException | LinkageError cleanupFailure)
                {
                    failure.addSuppressed(cleanupFailure);
                    throw failure;
                }
            }
            commands.remove(command);
            throw failure;
        }
    }

    @Override
    public void unregisterCommand(PlexCommand command)
    {
        Objects.requireNonNull(command, "command");
        commands.remove(command);
        api.commands().unregister(command);
    }

    @Override
    public List<PlexCommand> commands()
    {
        return List.copyOf(commands);
    }

    @Override
    public void registerListener(Listener listener)
    {
        Objects.requireNonNull(listener, "listener");
        if (!listeners.add(listener))
        {
            throw new IllegalStateException("Listener is already registered");
        }
        try
        {
            Plex.get().getServer().getPluginManager().registerEvents(listener, Plex.get());
        }
        catch (RuntimeException | LinkageError failure)
        {
            listeners.remove(listener);
            throw failure;
        }
    }

    @Override
    public void unregisterListener(Listener listener)
    {
        Objects.requireNonNull(listener, "listener");
        listeners.remove(listener);
        HandlerList.unregisterAll(listener);
    }

    @Override
    public List<Listener> listeners()
    {
        return List.copyOf(listeners);
    }

    void cleanupContributions()
    {
        for (PlexCommand command : commands())
        {
            try
            {
                unregisterCommand(command);
            }
            catch (RuntimeException | LinkageError failure)
            {
                PlexLog.error("Could not unregister a command from module " + descriptor.getName(), failure);
            }
        }
        for (Listener listener : listeners())
        {
            try
            {
                unregisterListener(listener);
            }
            catch (RuntimeException | LinkageError failure)
            {
                PlexLog.error("Could not unregister a listener from module " + descriptor.getName(), failure);
            }
        }
        try
        {
            acceptingTasks = false;
            tasks.forEach(ScheduledTask::cancel);
            tasks.clear();
        }
        catch (RuntimeException | LinkageError failure)
        {
            PlexLog.error("Could not cancel tasks for module " + descriptor.getName(), failure);
        }
    }

    CompletableFuture<Void> close()
    {
        return shutdownCompletion.handle((ignored, failure) ->
        {
            closeClassLoader(failure);
            return null;
        });
    }

    private void closeClassLoader(Throwable shutdownFailure)
    {
        if (shutdownFailure != null)
        {
            PlexLog.error("Asynchronous shutdown failed for module " + descriptor.getName(), shutdownFailure);
        }
        try
        {
            classLoader.close();
        }
        catch (IOException failure)
        {
            PlexLog.error("Could not close module " + descriptor.getName() + " classloader", failure);
        }
    }
}
