package dev.plex.services.impl;

import dev.plex.Plex;
import dev.plex.services.AbstractService;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import java.util.List;

public class AutoWipeService extends AbstractService
{
    public AutoWipeService()
    {
        super(true, false);
    }

    @Override
    public void run()
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
                        entity.remove();
                    }
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