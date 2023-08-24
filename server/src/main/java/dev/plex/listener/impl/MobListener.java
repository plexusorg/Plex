package dev.plex.listener.impl;

import dev.plex.listener.PlexListener;
import dev.plex.util.BlockUtils;
import dev.plex.util.PlexUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class MobListener extends PlexListener
{
    private static final List<Material> SPAWN_EGGS = Arrays.stream(Material.values()).filter((mat) -> mat.name().endsWith("_SPAWN_EGG")).toList();

    private static EntityType spawnEggToEntityType(Material mat)
    {
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

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event)
    {
        if (event.isCancelled()) return;
        if (event.getEntity().getEntitySpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER_EGG)
        {
            // for the future, we can instead filter and restrict nbt tags right here.
            // currently, however, the entity from spawn eggs are spawned by other event handlers
            event.setCancelled(true);
            return;
        }

        if (plugin.config.getStringList("blocked_entities").stream().anyMatch(type -> type.equalsIgnoreCase(event.getEntityType().name())))
        {
            event.setCancelled(true);
            Location location = event.getLocation();
            Collection<Player> coll = location.getNearbyEntitiesByType(Player.class, 10);
            PlexUtils.disabledEffectMultiple(coll.toArray(new Player[coll.size()]), location); // dont let intellij auto correct toArray to an empty array (for efficiency)
        }
    }

    @EventHandler
    public void onDispense(BlockDispenseEvent event)
    {
        ItemStack item = event.getItem();
        Material itemType = item.getType();
        if (SPAWN_EGGS.contains(itemType))
        {
            Block block = event.getBlock();
            Location blockLoc = BlockUtils.relative(block.getLocation(), ((Directional) block.getBlockData()).getFacing()).add(.5, 0, .5);
            EntityType eggType = spawnEggToEntityType(itemType);
            if (eggType != null)
            {
                blockLoc.getWorld().spawnEntity(blockLoc, eggType);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityClick(PlayerInteractEntityEvent event)
    {
        if (event.isCancelled()) return;
        Material handItem = event.getPlayer().getEquipment().getItem(event.getHand()).getType();
        if (event.getRightClicked() instanceof Ageable entity)
        {
            if (SPAWN_EGGS.contains(handItem))
            {
                EntityType eggType = spawnEggToEntityType(handItem);
                if (eggType != null)
                {
                    Entity spawned = entity.getWorld().spawnEntity(entity.getLocation(), eggType);
                    if (spawned instanceof Ageable ageable)
                    {
                        ageable.setBaby();
                    }
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event)
    {
        if (event.useItemInHand() == Event.Result.DENY) return;
        if (event.useInteractedBlock() == Event.Result.DENY) return;
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
            }
        }
    }
}
