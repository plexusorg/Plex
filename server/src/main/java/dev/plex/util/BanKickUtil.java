package dev.plex.util;

import org.bukkit.Bukkit;

import dev.plex.Plex;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.admission.BanDecisionService;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

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
        Bukkit.getGlobalRegionScheduler().run(plugin, task ->
        {
            for (Player player : List.copyOf(Bukkit.getOnlinePlayers()))
            {
                player.getScheduler().run(plugin, entityTask ->
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
                }, null);
            }
        });
    }

    public static CompletableFuture<String> currentOrLastIp(Plex plugin, PlexPlayer plexPlayer)
    {
        String lastIp = plexPlayer.getIps().isEmpty()
                ? ""
                : BanDecisionService.canonicalIp(plexPlayer.getIps().getLast());
        Player player = Bukkit.getPlayer(plexPlayer.getUuid());
        if (player == null)
        {
            return CompletableFuture.completedFuture(lastIp);
        }
        if (player.getAddress() == null || player.getAddress().getAddress() == null)
        {
            return CompletableFuture.completedFuture(lastIp);
        }
        return CompletableFuture.completedFuture(BanDecisionService.canonicalIp(player.getAddress().getAddress().getHostAddress()));
    }
}
