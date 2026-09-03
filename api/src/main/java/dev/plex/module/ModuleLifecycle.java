package dev.plex.module;

import dev.plex.api.PlexApi;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import dev.plex.command.PlexCommand;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;

interface ModuleLifecycle
{
    PlexApi api();

    Plugin plugin();

    <T extends ScheduledTask> @Nullable T ownTask(@Nullable T task);

    void kickPlayerOnShutdown(Player player, Component reason);

    void completeShutdownBeforeClose(CompletableFuture<Void> completion);

    void registerCommand(PlexCommand command);

    void unregisterCommand(PlexCommand command);

    List<PlexCommand> commands();

    void registerListener(Listener listener);

    void unregisterListener(Listener listener);

    List<Listener> listeners();
}
