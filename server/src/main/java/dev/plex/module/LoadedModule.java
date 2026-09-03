package dev.plex.module;

import dev.plex.api.PlexApi;
import dev.plex.api.listener.EventRule;
import dev.plex.api.scheduler.TaskScope;
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
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

final class LoadedModule implements ModuleLifecycle
{
    private final PlexModule module;
    private final PlexApi api;
    private final TaskScope scheduler;
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
        this.scheduler = api.scheduler().taskScope();
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
    public TaskScope scheduler()
    {
        return scheduler;
    }

    @Override
    public void kickPlayerOnShutdown(Player player, Component reason)
    {
        api.scheduler().executeEntity(player, () -> player.kick(reason), () -> { }, 0L);
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
            api.listeners().register(listener);
        }
        catch (RuntimeException | LinkageError failure)
        {
            listeners.remove(listener);
            throw failure;
        }
    }

    @Override
    public void registerListener(Listener listener, EventRule<?>... rules)
    {
        Objects.requireNonNull(rules, "rules");
        for (EventRule<?> rule : rules)
        {
            Objects.requireNonNull(rule, "rule");
        }
        registerListener(listener);
        try
        {
            api.listeners().register(listener, rules);
        }
        catch (RuntimeException | LinkageError failure)
        {
            try
            {
                unregisterListener(listener);
            }
            catch (RuntimeException | LinkageError cleanupFailure)
            {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    @Override
    public Listener registerEventRules(EventRule<?>... rules)
    {
        Listener listener = api.listeners().register(rules);
        listeners.add(listener);
        return listener;
    }

    @Override
    public void unregisterListener(Listener listener)
    {
        Objects.requireNonNull(listener, "listener");
        listeners.remove(listener);
        api.listeners().unregister(listener);
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
            scheduler.cancelAll();
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
