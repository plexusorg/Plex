package dev.plex.listener.impl;

import dev.plex.listener.PlexListener;
import java.util.ArrayList;
import java.util.List;

import dev.plex.util.PlexUtils;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
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

        Block block = event.getBlock();

        if (blockedPlayers.contains(event.getPlayer().getName()))
        {
            event.setCancelled(true);
        }

        if (blockedBlocks.contains(block.getType()))
        {
            block.setType(Material.CAKE);
            PlexUtils.disabledEffect(event.getPlayer(), block.getLocation().add(0.5,0.5,0.5));
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
