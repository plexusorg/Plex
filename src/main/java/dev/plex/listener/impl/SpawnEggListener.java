package dev.plex.listener.impl;

import dev.plex.listener.PlexListener;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.Arrays;
import java.util.List;

public class SpawnEggListener extends PlexListener
{
    public static final List<Material> SPAWN_EGGS = Arrays.stream(Material.values()).filter((mat) -> mat.name().endsWith("_SPAWN_EGG")).toList();

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
                EntityType eggType = null;
                try
                {
                    Material mat = event.getMaterial();
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
                    //
                }
                if (eggType != null)
                {
                    clickedBlock.getWorld().spawnEntity(clickedBlock.getLocation().add(event.getBlockFace().getDirection()).add(0.5, 0.5, 0.5), eggType);
                }
                return;
            }
        }
    }
}
