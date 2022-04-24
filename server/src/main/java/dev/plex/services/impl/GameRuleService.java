package dev.plex.services.impl;

import dev.plex.services.AbstractService;
import dev.plex.util.GameRuleUtil;
import dev.plex.util.PlexLog;
import java.util.Locale;
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
            GameRuleUtil.commitGlobalGameRules(world);
            PlexLog.log("Set global gamerules for world: " + world.getName());
        }
        for (String world : plugin.config.getConfigurationSection("worlds").getKeys(false))
        {
            World bukkitWorld = Bukkit.getWorld(world);
            if (bukkitWorld != null)
            {
                GameRuleUtil.commitSpecificGameRules(bukkitWorld);
                PlexLog.log("Set specific gamerules for world: " + world.toLowerCase(Locale.ROOT));
            }
        }
    }

    @Override
    public int repeatInSeconds()
    {
        return 0;
    }
}
