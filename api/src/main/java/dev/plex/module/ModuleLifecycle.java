package dev.plex.module;

import dev.plex.api.PlexApi;
import dev.plex.api.listener.EventRule;
import dev.plex.api.scheduler.TaskScope;
import dev.plex.command.PlexCommand;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

interface ModuleLifecycle
{
    PlexApi api();

    TaskScope scheduler();

    void kickPlayerOnShutdown(Player player, Component reason);

    void completeShutdownBeforeClose(CompletableFuture<Void> completion);

    void registerCommand(PlexCommand command);

    void unregisterCommand(PlexCommand command);

    List<PlexCommand> commands();

    void registerListener(Listener listener);

    void registerListener(Listener listener, EventRule<?>... rules);

    Listener registerEventRules(EventRule<?>... rules);

    void unregisterListener(Listener listener);

    List<Listener> listeners();
}
