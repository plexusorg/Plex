package me.totalfreedom.plex.listener.impl;

import me.totalfreedom.plex.cache.PlayerCache;
import me.totalfreedom.plex.listener.PlexListener;
import me.totalfreedom.plex.player.PunishedPlayer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerMoveEvent;

public class FreezeListener extends PlexListener
{
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e)
    {
        PunishedPlayer player = PlayerCache.getPunishedPlayer(e.getPlayer().getUniqueId());
        if (player.isFrozen())
            e.setCancelled(true);
    }
}