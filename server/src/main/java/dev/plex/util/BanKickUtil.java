package dev.plex.util;

import dev.plex.Plex;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.admission.BanDecisionService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class BanKickUtil
{
    private BanKickUtil()
    {
    }

    public static void kickBannedPlayers(Plex plugin, java.util.UUID uuid, String ip, Component message)
    {
        kickMatchingPlayers(plugin, uuid, ip, message);
    }

    public static void kickPlayersWithIp(Plex plugin, String ip, Component message)
    {
        kickMatchingPlayers(plugin, null, ip, message);
    }

    private static void kickMatchingPlayers(Plex plugin, java.util.UUID uuid, String ip, Component message)
    {
        String canonicalIp = BanDecisionService.canonicalIp(ip);
        plugin.getApi().scheduler().runGlobal(() ->
        {
            List<Player> onlinePlayers = List.copyOf(Bukkit.getOnlinePlayers());
            for (Player player : onlinePlayers)
            {
                plugin.getApi().scheduler().runEntity(player, () ->
                {
                    if (uuid != null && player.getUniqueId().equals(uuid))
                    {
                        BungeeUtil.kickPlayer(plugin, player, message);
                        return;
                    }
                    if (canonicalIp.isEmpty() || player.getAddress() == null || player.getAddress().getAddress() == null)
                    {
                        return;
                    }
                    String playerIp = BanDecisionService.canonicalIp(player.getAddress().getAddress().getHostAddress());
                    if (canonicalIp.equals(playerIp)) BungeeUtil.kickPlayer(plugin, player, message);
                });
            }
        });
    }

    public static CompletableFuture<String> currentOrLastIp(Plex plugin, PlexPlayer plexPlayer)
    {
        String lastIp = plexPlayer.getIps().isEmpty()
                ? ""
                : BanDecisionService.canonicalIp(plexPlayer.getIps().getLast());
        CompletableFuture<String> result = new CompletableFuture<>();
        plugin.getApi().scheduler().runGlobal(() ->
        {
            Player player = List.copyOf(Bukkit.getOnlinePlayers()).stream()
                    .filter(onlinePlayer -> onlinePlayer.getUniqueId().equals(plexPlayer.getUuid()))
                    .findFirst()
                    .orElse(null);
            if (player == null)
            {
                result.complete(lastIp);
                return;
            }
            ScheduledTask task = plugin.getApi().scheduler().runEntity(player, scheduledTask ->
            {
                if (player.getAddress() == null || player.getAddress().getAddress() == null)
                {
                    result.complete(lastIp);
                    return;
                }
                result.complete(BanDecisionService.canonicalIp(player.getAddress().getAddress().getHostAddress()));
            }, () -> result.complete(lastIp));
            if (task == null)
            {
                result.complete(lastIp);
            }
        });
        return result;
    }
}
