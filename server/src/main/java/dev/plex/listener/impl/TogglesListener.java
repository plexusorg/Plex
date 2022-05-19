package dev.plex.listener.impl;

import dev.plex.listener.PlexListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

public class TogglesListener extends PlexListener
{
    @EventHandler
    public void onEntityExplode(ExplosionPrimeEvent event)
    {
        if (!plugin.toggles.getBoolean("explosions"))
        {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFluidSpread(BlockFromToEvent event)
    {
        if (!plugin.toggles.getBoolean("fluidspread"))
        {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFluidSpread(PlayerDropItemEvent event)
    {
        if (!plugin.toggles.getBoolean("drops"))
        {
            event.setCancelled(true);
        }
    }
}
