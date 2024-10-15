package dev.plex.listener.impl;

import dev.plex.listener.PlexListener;
import dev.plex.services.impl.TimingService;
import dev.plex.util.PlexUtils;
import java.util.UUID;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class AntiNukerListener extends PlexListener
{
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event)
    {
        TimingService.nukerCooldown.merge(event.getPlayer().getUniqueId(), 1L, Long::sum);
        if (getCount(event.getPlayer().getUniqueId()) > 200L)
        {
            TimingService.strikes.merge(event.getPlayer().getUniqueId(), 1L, Long::sum);
            event.getPlayer().kick(PlexUtils.messageComponent("nukerKickMessage"));
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event)
    {
        TimingService.nukerCooldown.merge(event.getPlayer().getUniqueId(), 1L, Long::sum);
        if (getCount(event.getPlayer().getUniqueId()) > 200L)
        {
            TimingService.strikes.merge(event.getPlayer().getUniqueId(), 1L, Long::sum);
            event.getPlayer().kick(PlexUtils.messageComponent("nukerKickMessage"));
            event.setCancelled(true);
        }
    }

    public long getCount(UUID uuid)
    {
        return TimingService.nukerCooldown.getOrDefault(uuid, 1L);
    }
}
