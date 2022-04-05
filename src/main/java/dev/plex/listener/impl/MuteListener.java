package dev.plex.listener.impl;

import dev.plex.cache.DataUtils;
import dev.plex.cache.player.PlayerCache;
import dev.plex.listener.PlexListener;
import dev.plex.util.PlexUtils;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

public class MuteListener extends PlexListener
{

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event)
    {
        if (DataUtils.getPlayer(event.getPlayer().getUniqueId()).isMuted())
        {
            event.getPlayer().sendMessage(PlexUtils.messageComponent("muted"));
            event.setCancelled(true);
        }
    }
}
