package dev.plex.util;

import org.bukkit.Bukkit;

import dev.plex.Plex;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.admission.BanDecisionService;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public final class BanKickUtil
{
    private BanKickUtil()
    {
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
