package dev.plex.listener.impl;


import dev.plex.listener.ServerListenerBase;
import dev.plex.player.PlexPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class FreezeListener extends ServerListenerBase
{
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e)
    {
        PlexPlayer player = plugin.getPlayerService().getPlayer(e.getPlayer().getUniqueId());
        if (player.isFrozen())
        {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent e)
    {
        PlexPlayer player = plugin.getPlayerService().getPlayer(e.getPlayer().getUniqueId());
        if (player.isFrozen())
        {
            e.setCancelled(true);
        }
    }
}