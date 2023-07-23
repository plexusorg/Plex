package dev.plex.services.impl;

import dev.plex.Plex;
import dev.plex.services.AbstractService;
import dev.plex.util.PlexLog;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;

public class AutoWipeService extends AbstractService
{
    public AutoWipeService()
    {
        super(true, false);
    }

    @Override
    public void run(ScheduledTask task)
    {
        if (Plex.get().config.getBoolean("autowipe.enabled"))
        {
            List<String> entities = plugin.config.getStringList("autowipe.entities");

            for (World world : Bukkit.getWorlds())
            {
                for (Entity entity : world.getEntities())
                {
                    if (entities.stream().anyMatch(entityName -> entityName.equalsIgnoreCase(entity.getType().name())))
                    {
                        Bukkit.getRegionScheduler().run(Plex.get(), entity.getLocation(), this::entityRun);
                    }
                }
            }
        }
    }

    private void entityRun(ScheduledTask task)
    {
        List<String> entities = plugin.config.getStringList("autowipe.entities");

        for (World world : Bukkit.getWorlds())
        {
            for (Entity entity : world.getEntities())
            {
                if (entities.stream().anyMatch(entityName -> entityName.equalsIgnoreCase(entity.getType().name())))
                {
                    entity.remove();
                    task.cancel();
                }
            }
        }
    }

    @Override
    public int repeatInSeconds()
    {
        return Math.max(1, plugin.config.getInt("autowipe.interval"));
    }
}