package dev.plex.services.impl;

import org.bukkit.Bukkit;

import dev.plex.Plex;
import dev.plex.util.PlexLog;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityRemoveEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;

public class AutoWipeService implements Listener
{
    private final Plex plugin;
    private final Map<UUID, Entity> entities = new ConcurrentHashMap<>();
    private Set<EntityType> entityTypes = Set.of();
    private volatile ScheduledTask task;

    public AutoWipeService(Plex plugin)
    {
        this.plugin = plugin;
    }

    public synchronized void start()
    {
        stop();
        if (!plugin.entities.getBoolean("autowipe.enabled"))
        {
            return;
        }
        entityTypes = configuredTypes();
        Bukkit.getPluginManager().registerEvents(this, plugin);
        for (World world : Bukkit.getWorlds())
        {
            for (Chunk chunk : world.getLoadedChunks())
            {
                Bukkit.getRegionScheduler().execute(plugin, world, chunk.getX(), chunk.getZ(), () ->
                {
                    if (chunk.isLoaded()) index(chunk);
                });
            }
        }
        task = Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, this::run, 1L, 20L * repeatInSeconds());
    }

    public synchronized void stop()
    {
        if (task != null)
        {
            task.cancel();
            task = null;
        }
        HandlerList.unregisterAll(this);
        entities.clear();
    }

    public boolean isRunning()
    {
        return task != null;
    }

    public synchronized void run(ScheduledTask task)
    {
        if (this.task != task)
        {
            return;
        }
        entities.forEach((uuid, entity) -> entity.getScheduler().run(plugin, ignored ->
        {
            if (entityTypes.contains(entity.getType())) entity.remove();
            entities.remove(uuid, entity);
        }, () -> entities.remove(uuid, entity)));
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event)
    {
        index(event.getEntity());
    }

    @EventHandler
    public void onEntityRemove(EntityRemoveEvent event)
    {
        entities.remove(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onChunkLoad(ChunkLoadEvent event)
    {
        index(event.getChunk());
    }

    @EventHandler
    public void onChunkUnload(ChunkUnloadEvent event)
    {
        for (Entity entity : event.getChunk().getEntities())
        {
            entities.remove(entity.getUniqueId());
        }
    }

    private int repeatInSeconds()
    {
        return Math.max(1, plugin.entities.getInt("autowipe.interval"));
    }

    private void index(Chunk chunk)
    {
        for (Entity entity : chunk.getEntities()) index(entity);
    }

    private void index(Entity entity)
    {
        if (entityTypes.contains(entity.getType())) entities.put(entity.getUniqueId(), entity);
    }

    private Set<EntityType> configuredTypes()
    {
        EnumSet<EntityType> parsed = EnumSet.noneOf(EntityType.class);
        for (String configuredName : plugin.entities.getStringList("autowipe.entities"))
        {
            String name = configuredName.trim().toUpperCase(Locale.ROOT);
            if (name.equals("DROPPED_ITEM")) name = "ITEM";
            try
            {
                parsed.add(EntityType.valueOf(name));
            }
            catch (IllegalArgumentException exception)
            {
                PlexLog.warn("Ignoring unknown autowipe entity type: {0}", configuredName);
            }
        }
        return Set.copyOf(parsed);
    }
}
