package dev.plex.util;

import dev.plex.Plex;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class GameModeUtil
{
    private GameModeUtil()
    {
    }

    public static CompletableFuture<Void> update(Plex plugin, CommandSender sender, Player target, GameMode gameMode)
    {
        CompletableFuture<Void> completion = new CompletableFuture<>();
        ScheduledTask task = plugin.getApi().scheduler().runEntity(target, scheduledTask ->
        {
            target.setGameMode(gameMode);
            String modeName = gameMode.toString().toLowerCase();
            if (sender.getName().equals(target.getName()))
            {
                sender.sendMessage(PlexUtils.messageComponent("gameModeSetTo", modeName));
            }
            else
            {
                target.sendMessage(PlexUtils.messageComponent("playerSetOtherGameMode", sender.getName(), modeName));
                sender.sendMessage(PlexUtils.messageComponent("setOtherPlayerGameModeTo", target.getName(), modeName));
            }
            completion.complete(null);
        }, () -> completion.complete(null));
        if (task == null)
        {
            completion.complete(null);
        }
        return completion;
    }

    public static CompletableFuture<Void> updateAll(Plex plugin, GameMode gameMode, boolean notifyTargets)
    {
        CompletableFuture<Void> completion = new CompletableFuture<>();
        plugin.getApi().scheduler().runGlobal(() ->
        {
            List<CompletableFuture<Void>> updates = List.copyOf(Bukkit.getOnlinePlayers()).stream()
                    .map(player -> updateOne(plugin, player, gameMode, notifyTargets))
                    .toList();
            CompletableFuture.allOf(updates.toArray(CompletableFuture[]::new))
                    .whenComplete((unused, failure) ->
                    {
                        if (failure == null) completion.complete(null);
                        else completion.completeExceptionally(failure);
                    });
        });
        return completion;
    }

    private static CompletableFuture<Void> updateOne(Plex plugin, Player player, GameMode gameMode, boolean notify)
    {
        CompletableFuture<Void> completion = new CompletableFuture<>();
        ScheduledTask task = plugin.getApi().scheduler().runEntity(player, scheduledTask ->
        {
            player.setGameMode(gameMode);
            if (notify) player.sendMessage(PlexUtils.messageComponent("gameModeSetTo", gameMode.toString().toLowerCase()));
            completion.complete(null);
        }, () -> completion.complete(null));
        if (task == null)
        {
            completion.complete(null);
        }
        return completion;
    }
}
