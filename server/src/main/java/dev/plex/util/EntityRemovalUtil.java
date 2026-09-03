package dev.plex.util;

import org.bukkit.Bukkit;

import dev.plex.Plex;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public final class EntityRemovalUtil
{
    private EntityRemovalUtil()
    {
    }

    public static CompletableFuture<Map<String, Integer>> removeLoaded(Plex plugin, Predicate<Entity> selected)
    {
        CompletableFuture<Map<String, Integer>> completion = new CompletableFuture<>();
        Bukkit.getGlobalRegionScheduler().run(plugin, task ->
        {
            List<Chunk> chunks = new ArrayList<>();
            for (World world : Bukkit.getWorlds())
            {
                chunks.addAll(List.of(world.getLoadedChunks()));
            }
            if (chunks.isEmpty())
            {
                completion.complete(Map.of());
                return;
            }

            Map<String, Integer> counts = new ConcurrentHashMap<>();
            AtomicInteger remaining = new AtomicInteger(chunks.size());
            for (Chunk chunk : chunks)
            {
                Bukkit.getRegionScheduler().run(plugin, chunk.getWorld(), chunk.getX(), chunk.getZ(), regionTask ->
                {
                    for (Entity entity : chunk.getEntities())
                    {
                        if (!selected.test(entity)) continue;
                        String type = entity.getType().name();
                        entity.remove();
                        counts.merge(type, 1, Integer::sum);
                    }
                    if (remaining.decrementAndGet() == 0) completion.complete(new HashMap<>(counts));
                });
            }
        });
        return completion;
    }
}
