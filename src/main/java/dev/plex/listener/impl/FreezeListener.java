package dev.plex.listener.impl;

import dev.plex.cache.DataUtils;
import dev.plex.listener.PlexListener;
import dev.plex.player.PlexPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;

public class FreezeListener extends PlexListener
{
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e)
    {
        PlexPlayer player = DataUtils.getPlayer(e.getPlayer().getUniqueId());
        if (player.isFrozen())
        {
            e.setCancelled(true);
        }
    }
}