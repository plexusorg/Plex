package dev.plex.services.impl;

import dev.plex.services.AbstractService;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;

public class GameRuleService extends AbstractService
{
    public GameRuleService()
    {
        super(false, true);
    }

    @Override
    public void run()
    {
        for (World world : Bukkit.getWorlds())
        {
            PlexUtils.commitGameRules(world);
            PlexLog.debug("Set gamerules for world: " + world.getName());
        }
    }

    @Override
    public int repeatInSeconds()
    {
        return 0;
    }
}
