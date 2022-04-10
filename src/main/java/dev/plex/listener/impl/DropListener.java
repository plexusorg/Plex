package dev.plex.listener.impl;

import dev.plex.listener.PlexListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerDropItemEvent;

public class DropListener extends PlexListener
{
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event)
    {
        if (!plugin.config.getBoolean("allowdrops"))
        {
            event.setCancelled(true);
        }
    }
}
