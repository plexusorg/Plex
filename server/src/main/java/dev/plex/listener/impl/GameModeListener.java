package dev.plex.listener.impl;

import dev.plex.Plex;
import dev.plex.listener.ServerListenerBase;
import dev.plex.util.PlexUtils;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class GameModeListener extends ServerListenerBase
{
    public GameModeListener(Plex plugin)
    {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event)
    {
        Player player = event.getPlayer();
        if (plugin.getPunishmentManager().isFiniteBanRestricted(player.getUniqueId())
                && event.getNewGameMode() != GameMode.SPECTATOR)
        {
            event.setCancelled(true);
            event.cancelMessage(plugin.getPunishmentManager().finiteBanMessage(player.getUniqueId()));
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onSpectatorTeleport(PlayerTeleportEvent event)
    {
        Player player = event.getPlayer();
        if (plugin.getPunishmentManager().isFiniteBanRestricted(player.getUniqueId()))
        {
            event.setCancelled(true);
            player.sendMessage(plugin.getPunishmentManager().finiteBanMessage(player.getUniqueId()));
            return;
        }
        if (event.getCause() != PlayerTeleportEvent.TeleportCause.SPECTATE) return;
        if (!player.hasPermission("plex.gamemode.spectator.teleport"))
        {
            event.setCancelled(true);
            player.sendMessage(PlexUtils.messageComponent("spectatorTeleportDenied"));
        }
    }
}
