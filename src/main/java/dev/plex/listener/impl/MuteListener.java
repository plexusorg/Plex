package dev.plex.listener.impl;

import dev.plex.cache.PlayerCache;
import dev.plex.listener.PlexListener;
import dev.plex.util.PlexUtils;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.event.EventHandler;

public class MuteListener extends PlexListener
{

    @EventHandler
    public void onChat(AsyncChatEvent event)
    {
        if (PlayerCache.getPunishedPlayer(event.getPlayer().getUniqueId()).isMuted())
        {
            event.getPlayer().sendMessage(PlexUtils.messageComponent("muted"));
            event.setCancelled(true);
        }
    }
}
