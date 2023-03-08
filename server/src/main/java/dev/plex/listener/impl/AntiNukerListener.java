package dev.plex.listener.impl;

import dev.plex.listener.PlexListener;
import dev.plex.services.impl.TimingService;
import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.UUID;

public class AntiNukerListener extends PlexListener
{
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event)
    {
        TimingService.nukerCooldown.merge(event.getPlayer().getUniqueId(), 1L, Long::sum);
        if (getCount(event.getPlayer().getUniqueId()) > 200L)
        {
            TimingService.strikes.merge(event.getPlayer().getUniqueId(), 1L, Long::sum);
            event.getPlayer().kick(Component.text("Please turn off your nuker!"));
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
            event.getPlayer().kick(Component.text("Please turn off your nuker!"));
            event.setCancelled(true);
        }
    }

    public long getCount(UUID uuid)
    {
        return TimingService.nukerCooldown.getOrDefault(uuid, 1L);
    }
}
