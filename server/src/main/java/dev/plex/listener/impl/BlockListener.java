package dev.plex.listener.impl;

import dev.plex.listener.PlexListener;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BlockListener extends PlexListener
{
    private static final List<Material> blockedBlocks = new ArrayList<>();
    private static final List<Material> SIGNS = Arrays.stream(Material.values()).filter((mat) -> mat.name().endsWith("_SIGN")).toList();
    private static List<String> cachedBlockedBlocksConfig = null;
    public List<String> blockedPlayers = new ArrayList<>();

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockPlace(BlockPlaceEvent event)
    {
        List<String> blockedBlocksConfig = plugin.config.getStringList("blocked_blocks");
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
                catch (IllegalArgumentException ignored)
                {
                }
            }
        }

        Block block = event.getBlock();

        if (blockedPlayers.contains(event.getPlayer().getName()))
        {
            event.setCancelled(true);
            return;
        }

        if (blockedBlocks.contains(block.getType()))
        {
            block.setType(Material.CAKE);
            PlexUtils.disabledEffect(event.getPlayer(), block.getLocation().add(0.5, 0.5, 0.5));
        }

        if (SIGNS.contains(block.getType()))
        {
            Sign sign = (Sign) block.getState();
            boolean anythingChanged = false;
            for (int i = 0; i < sign.lines().size(); i++)
            {
                Component line = sign.line(i);
                if (line.clickEvent() != null)
                {
                    anythingChanged = true;
                    sign.line(i, line.clickEvent(null));
                }
            }
            if (anythingChanged)
            {
                sign.update(true);
                PlexUtils.disabledEffect(event.getPlayer(), block.getLocation().add(0.5, 0.5, 0.5));
            }
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
