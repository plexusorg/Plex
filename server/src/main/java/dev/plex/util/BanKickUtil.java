package dev.plex.util;

import dev.plex.Plex;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.admission.BanDecisionService;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class BanKickUtil
{
    private BanKickUtil()
    {
    }

    public static void kickPlayersWithIp(Plex plugin, String ip, Component message)
    {
        String canonicalIp = BanDecisionService.canonicalIp(ip);
        if (canonicalIp.isEmpty())
        {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers())
        {
            if (player.getAddress() == null || player.getAddress().getAddress() == null)
            {
                continue;
            }
            String playerIp = BanDecisionService.canonicalIp(player.getAddress().getAddress().getHostAddress());
            if (canonicalIp.equals(playerIp))
            {
                plugin.getApi().scheduler().runEntity(player, () -> BungeeUtil.kickPlayer(plugin, player, message));
            }
        }
    }

    public static String currentOrLastIp(PlexPlayer plexPlayer)
    {
        Player onlinePlayer = Bukkit.getPlayer(plexPlayer.getUuid());
        if (onlinePlayer != null && onlinePlayer.getAddress() != null && onlinePlayer.getAddress().getAddress() != null)
        {
            return BanDecisionService.canonicalIp(onlinePlayer.getAddress().getAddress().getHostAddress());
        }
        return plexPlayer.getIps().isEmpty()
                ? ""
                : BanDecisionService.canonicalIp(plexPlayer.getIps().getLast());
    }
}
