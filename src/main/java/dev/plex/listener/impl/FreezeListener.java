package dev.plex.listener.impl;

import dev.plex.cache.PlayerCache;
import dev.plex.listener.PlexListener;
import dev.plex.player.PunishedPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;

public class FreezeListener extends PlexListener
{
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e)
    {
        PunishedPlayer player = PlayerCache.getPunishedPlayer(e.getPlayer().getUniqueId());
        if (player.isFrozen())
        {
            e.setCancelled(true);
        }
    }

}