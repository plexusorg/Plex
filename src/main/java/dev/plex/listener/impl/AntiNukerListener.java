package dev.plex.listener.impl;

import dev.plex.listener.PlexListener;
import dev.plex.services.impl.TimingService;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class AntiNukerListener extends PlexListener
{
    @EventHandler(priority = EventPriority.LOW)
    public void onBlockPlace(BlockPlaceEvent event)
    {
        TimingService.nukerCooldown.merge(event.getPlayer().getUniqueId(), 1L, Long::sum);
        if (getCount(event.getPlayer().getUniqueId()) > 200L)
        {
            TimingService.strikes.merge(event.getPlayer().getUniqueId(), 1L, Long::sum);
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockBreak(BlockBreakEvent event)
    {
        TimingService.nukerCooldown.merge(event.getPlayer().getUniqueId(), 1L, Long::sum);
        if (getCount(event.getPlayer().getUniqueId()) > 200L)
        {
            TimingService.strikes.merge(event.getPlayer().getUniqueId(), 1L, Long::sum);
            event.setCancelled(true);
        }
    }

    public long getCount(UUID uuid)
    {
        return TimingService.nukerCooldown.getOrDefault(uuid, 1L);
    }
}
