package dev.plex.listener.impl;

import dev.plex.listener.PlexListener;
import java.util.ArrayList;
import java.util.List;

import net.kyori.adventure.sound.Sound;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockListener extends PlexListener
{
    public List<String> blockedPlayers = new ArrayList<>();

    private static final List<Material> blockedBlocks = new ArrayList<>();

    private static List<String> cachedBlockedBlocksConfig = null;

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockPlace(BlockPlaceEvent event)
    {
        List<String> blockedBlocksConfig = plugin.config.getStringList("blockedBlocks");
        if (blockedBlocksConfig != cachedBlockedBlocksConfig)
        {
            blockedBlocks.clear();
            cachedBlockedBlocksConfig = blockedBlocksConfig;
            for (String block : blockedBlocksConfig)
            {
                try
                {
                    blockedBlocks.add(Material.valueOf(block.toUpperCase()));
                }
                catch (IllegalArgumentException e)
                {
                    //
                }
            }
        }

        if (blockedBlocks.contains(event.getBlock().getType()))
        {
            event.getBlock().setType(Material.COBWEB);
            Particle.CLOUD.builder().location(event.getBlock().getLocation().add(0.5,0.5,0.5)).receivers(event.getPlayer()).extra(0).offset(0.5,0.5,0.5).count(5).spawn();
            Particle.FLAME.builder().location(event.getBlock().getLocation().add(0.5,0.5,0.5)).receivers(event.getPlayer()).extra(0).offset(0.5,0.5,0.5).count(3).spawn();
            Particle.SOUL_FIRE_FLAME.builder().location(event.getBlock().getLocation().add(0.5,0.5,0.5)).receivers(event.getPlayer()).offset(0.5,0.5,0.5).extra(0).count(2).spawn();
            event.getPlayer().playSound(Sound.sound(org.bukkit.Sound.BLOCK_FIRE_EXTINGUISH.key(), Sound.Source.BLOCK, 0.5f, 0.5f));
        }

        if (blockedPlayers.size() == 0)
        {
            return;
        }

        if (blockedPlayers.contains(event.getPlayer().getName()))
        {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockBreak(BlockBreakEvent event)
    {
        if (blockedPlayers.size() == 0)
        {
            return;
        }
        if (blockedPlayers.contains(event.getPlayer().getName()))
        {
            event.setCancelled(true);
        }
    }
}
