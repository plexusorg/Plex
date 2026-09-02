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
        for (Player player : Bukkit.getOnlinePlayers())
        {
            boolean uuidMatch = uuid != null && player.getUniqueId().equals(uuid);
            if (!uuidMatch && (canonicalIp.isEmpty() || player.getAddress() == null || player.getAddress().getAddress() == null))
            {
                continue;
            }
            boolean ipMatch = !canonicalIp.isEmpty() && player.getAddress() != null
                    && player.getAddress().getAddress() != null && canonicalIp.equals(
                    BanDecisionService.canonicalIp(player.getAddress().getAddress().getHostAddress()));
            if (uuidMatch || ipMatch)
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
