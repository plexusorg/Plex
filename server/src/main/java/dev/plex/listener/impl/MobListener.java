package dev.plex.listener.impl;

import dev.plex.Plex;
import dev.plex.listener.ServerListenerBase;
import dev.plex.util.PlexUtils;
import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.event.block.BlockPreDispenseEvent;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Collection;

public class MobListener extends ServerListenerBase
{
    public MobListener(Plex plugin)
    {
        super(plugin);
    }

    private static final DataComponentType ENTITY_DATA = RegistryAccess.registryAccess()
            .getRegistry(RegistryKey.DATA_COMPONENT_TYPE).getOrThrow(NamespacedKey.minecraft("entity_data"));

    private static void sanitizeEgg(ItemStack item)
    {
        if (item != null && item.getType().name().endsWith("_SPAWN_EGG"))
        {
            // Restore the egg's default entity type and discard custom entity data.
            item.resetData(ENTITY_DATA);
        }
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event)
    {
        if (event.isCancelled())
        {
            return;
        }
        if (plugin.entities.getStringList("blocked_entities").stream().anyMatch(type -> type.equalsIgnoreCase(event.getEntityType().name())))
        {
            event.setCancelled(true);
            Location location = event.getLocation();
            Collection<Player> coll = location.getNearbyEntitiesByType(Player.class, 10);
            PlexUtils.disabledEffectMultiple(coll.toArray(new Player[coll.size()]), location); // dont let intellij auto correct toArray to an empty array (for efficiency)
        }

        if (plugin.entities.getBoolean("entity_limit.mob_limit_enabled"))
        {
            Location location = event.getLocation();
            Chunk chunk = location.getChunk();

            if (isEntityLimitReached(chunk, plugin.entities.getInt("entity_limit.max_mobs_per_chunk")))
            {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDispense(BlockPreDispenseEvent event)
    {
        sanitizeEgg(event.getItemStack());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityClick(PlayerInteractEntityEvent event)
    {
        sanitizeEgg(event.getPlayer().getInventory().getItem(event.getHand()));
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerInteract(PlayerInteractEvent event)
    {
        if (event.useItemInHand() == Event.Result.DENY)
        {
            return;
        }
        if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK)
        {
            sanitizeEgg(event.getItem());
        }
    }

    public static boolean isEntityLimitReached(Chunk chunk, int limit)
    {
        return Arrays.stream(chunk.getEntities())
                .filter(entity -> entity instanceof LivingEntity && !(entity instanceof Player))
                .count() >= limit;
    }
}
