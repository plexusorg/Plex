package dev.plex.services.impl;

import dev.plex.services.AbstractService;
import dev.plex.util.PlexLog;
import org.bukkit.Bukkit;
import org.bukkit.GameRule;
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
            commitGameRules(world);
            PlexLog.debug("Set gamerules for world: " + world.getName());
        }
    }

    private void commitGameRules(World world)
    {
        world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
        world.setGameRule(GameRule.DO_FIRE_TICK, false);
        world.setGameRule(GameRule.KEEP_INVENTORY, true);
        world.setGameRule(GameRule.DO_MOB_LOOT, false);
        world.setGameRule(GameRule.MOB_GRIEFING, false);
        world.setGameRule(GameRule.DO_TILE_DROPS, false);
        world.setGameRule(GameRule.COMMAND_BLOCK_OUTPUT, false);
        world.setGameRule(GameRule.NATURAL_REGENERATION, true);
        world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        world.setGameRule(GameRule.ANNOUNCE_ADVANCEMENTS, false);
        world.setGameRule(GameRule.SHOW_DEATH_MESSAGES, false);
        world.setGameRule(GameRule.SEND_COMMAND_FEEDBACK, false);
        world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
    }

    @Override
    public int repeatInSeconds()
    {
        return 0;
    }
}
