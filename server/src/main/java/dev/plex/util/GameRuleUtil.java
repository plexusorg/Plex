package dev.plex.util;

import dev.plex.Plex;
import org.apache.commons.lang3.math.NumberUtils;
import org.bukkit.GameRule;
import org.bukkit.World;

import java.util.Locale;

public class GameRuleUtil
{
    public static <T> void commitGlobalGameRules(World world)
    {
        for (String s : Plex.get().config.getStringList("global_gamerules"))
        {
            readGameRules(world, s);
        }
    }

    public static <T> void commitSpecificGameRules(World world)
    {
        for (String s : Plex.get().config.getStringList("worlds." + world.getName().toLowerCase(Locale.ROOT) + ".gameRules"))
        {
            readGameRules(world, s);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void readGameRules(World world, String s)
    {
        String gameRule = s.split(";")[0];
        T value = (T) s.split(";")[1];
        GameRule<T> rule = (GameRule<T>) GameRule.getByName(gameRule);
        if (rule != null && check(value).getClass().equals(rule.getType()))
        {
            world.setGameRule(rule, value);
            PlexLog.debug("Setting game rule " + gameRule + " for world " + world.getName() + " with value " + value);
        }
        else
        {
            PlexLog.error(String.format("Failed to set game rule %s for world %s with value %s!", gameRule, world.getName().toLowerCase(Locale.ROOT), value));
        }
    }

    public static <T> Object check(T value)
    {
        if (value.toString().equalsIgnoreCase("true") || value.toString().equalsIgnoreCase("false"))
        {
            return Boolean.parseBoolean(value.toString());
        }

        if (NumberUtils.isCreatable(value.toString()))
        {
            return Integer.parseInt(value.toString());
        }
        return value;
    }
}
