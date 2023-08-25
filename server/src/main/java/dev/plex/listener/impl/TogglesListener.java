package dev.plex.listener.impl;

import dev.plex.listener.PlexListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.ExplosionPrimeEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

public class TogglesListener extends PlexListener
{
    @EventHandler
    public void onExplosionPrime(ExplosionPrimeEvent event)
    {
        if (!plugin.toggles.getBoolean("explosions"))
        {
            event.getEntity().remove();
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        if (!plugin.toggles.getBoolean("explosions"))
        {
            event.getBlock().breakNaturally();
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event)
    {
        if (!plugin.toggles.getBoolean("explosions"))
        {
            event.getEntity().remove();
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

    /* I have no idea if this is the best way to do this
    There is a very weird bug where if you try to create a loop using two repeaters and a lever, after disabling
    and re-enabling redstone, you are unable to recreate the loop with a lever. Using a redstone torch works fine.
    Using a lever works fine also as long as you never toggle redstone.
     */
    @EventHandler
    public void onBlockRedstone(BlockRedstoneEvent event)
    {
        if (!plugin.toggles.getBoolean("redstone"))
        {
            event.setNewCurrent(0);
        }
    }
}
