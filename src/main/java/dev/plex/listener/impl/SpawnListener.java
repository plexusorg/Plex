package dev.plex.listener.impl;

import dev.plex.listener.PlexListener;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

public class SpawnListener extends PlexListener
{
    // TODO: CONFIGURABILITY!!!

    public static final List<Material> SPAWN_EGGS = Arrays.stream(Material.values()).filter((mat) -> mat.name().endsWith("_SPAWN_EGG")).toList();

    @EventHandler
    public void onDispense(BlockDispenseEvent event)
    {
        ItemStack item = event.getItem();
        Material itemType = item.getType();
        if (SPAWN_EGGS.contains(itemType))
        {
            Block block = event.getBlock();
            Location blockLoc = block.getLocation().add(0.5,0.5,0.5).add(((Directional) block.getBlockData()).getFacing().getDirection().multiply(0.8));
            event.setCancelled(true);
            item.setAmount(item.getAmount() - 1);
            event.setItem(item);
            EntityType eggType = spawnEggToEntityType(itemType);
            if (eggType != null)
            {
                blockLoc.getWorld().spawnEntity(blockLoc, eggType);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event)
    {
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
        {
            if (SPAWN_EGGS.contains(event.getMaterial()))
            {
                event.setCancelled(true);
                Block clickedBlock = event.getClickedBlock();
                if (clickedBlock == null)
                {
                    return;
                }
                EntityType eggType = spawnEggToEntityType(event.getMaterial());
                if (eggType != null)
                {
                    clickedBlock.getWorld().spawnEntity(clickedBlock.getLocation().add(event.getBlockFace().getDirection().multiply(0.8)).add(0.5, 0.5, 0.5), eggType);
                }
                return;
            }
        }
    }

    private static EntityType spawnEggToEntityType(Material mat){
        EntityType eggType;
        try
        {
            if (mat == Material.MOOSHROOM_SPAWN_EGG)
            {
                eggType = EntityType.MUSHROOM_COW;
            }
            else
            {
                eggType = EntityType.valueOf(mat.name().substring(0, mat.name().length() - 10));
            }
        }
        catch (IllegalArgumentException ignored)
        {
            return null;
        }
        return eggType;
    }
}
