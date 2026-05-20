package dev.plex.listener.impl;

import dev.plex.Plex;
import dev.plex.listener.ServerListenerBase;
import dev.plex.services.impl.TimingService;
import dev.plex.util.PlexUtils;
import io.papermc.paper.event.player.AsyncChatEvent;

import java.util.UUID;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class AntiSpamListener extends ServerListenerBase
{
    public AntiSpamListener(Plex plugin)
    {
        super(plugin);
    }

    @EventHandler
    public void onChat(AsyncChatEvent event)
    {
        TimingService.spamCooldown.merge(event.getPlayer().getUniqueId(), 1L, Long::sum);
        if (getCount(event.getPlayer().getUniqueId()) > 8L)
        {
            event.getPlayer().sendMessage(PlexUtils.messageComponent("antiSpamMessage"));
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event)
    {
        TimingService.spamCooldown.merge(event.getPlayer().getUniqueId(), 1L, Long::sum);
        if (getCount(event.getPlayer().getUniqueId()) > 8L)
        {
            event.getPlayer().sendMessage(PlexUtils.messageComponent("antiSpamMessage"));
            event.setCancelled(true);
        }
    }

    public long getCount(UUID uuid)
    {
        return TimingService.spamCooldown.getOrDefault(uuid, 1L);
    }
}
