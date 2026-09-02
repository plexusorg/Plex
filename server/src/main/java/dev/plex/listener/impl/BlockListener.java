package dev.plex.listener.impl;

import dev.plex.Plex;
import dev.plex.listener.ServerListenerBase;
import dev.plex.util.PlexUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockListener extends ServerListenerBase
{
    public BlockListener(Plex plugin)
    {
        super(plugin);
    }

    private static final List<Material> SIGNS = Arrays.stream(Material.values()).filter((mat) -> mat.name().endsWith("_SIGN")).toList();
    public static final List<String> blockedPlayers = new ArrayList<>();

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockPlace(BlockPlaceEvent event)
    {
        Block block = event.getBlock();

        if (blockedPlayers.contains(event.getPlayer().getName()))
        {
            event.setCancelled(true);
            return;
        }

        if (plugin.entities.getStringList("blocked_blocks").stream()
                .anyMatch(configuredBlock -> configuredBlock.equalsIgnoreCase(block.getType().name())))
        {
            block.setType(Material.CAKE);
            PlexUtils.disabledEffect(event.getPlayer(), block.getLocation().add(0.5, 0.5, 0.5));
        }

        if (SIGNS.contains(block.getType()))
        {
            Sign sign = (Sign) block.getState();
            boolean changed = false;
            for (Side side : Side.values())
            {
                for (int lineNumber = 0; lineNumber < sign.getSide(side).lines().size(); lineNumber++)
                {
                    Component line = sign.getSide(side).line(lineNumber);
                    if (line.clickEvent() != null)
                    {
                        sign.getSide(side).line(lineNumber, line.clickEvent(null));
                        changed = true;
                    }
                }
            }
            if (changed)
            {
                sign.update(true);
                PlexUtils.disabledEffect(event.getPlayer(), block.getLocation().add(0.5, 0.5, 0.5));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockBreak(BlockBreakEvent event)
    {
        if (blockedPlayers.isEmpty())
        {
            return;
        }
        if (blockedPlayers.contains(event.getPlayer().getName()))
        {
            event.setCancelled(true);
        }
    }
}
