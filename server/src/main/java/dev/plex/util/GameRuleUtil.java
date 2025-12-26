package dev.plex.util;

import dev.plex.Plex;

import java.util.Locale;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import org.bukkit.GameRule;
import org.bukkit.Registry;
import org.bukkit.World;

public class GameRuleUtil
{
    public static void commitGlobalGameRules(World world)
    {
        for (String s : Plex.get().config.getStringList("global_gamerules"))
        {
            readGameRules(world, s);
        }
    }

    public static void commitSpecificGameRules(World world)
    {
        for (String s : Plex.get().config.getStringList("worlds." + world.getName().toLowerCase(Locale.ROOT) + ".gameRules"))
        {
            readGameRules(world, s);
        }
    }

    private static void readGameRules(World world, String s)
    {
        String[] parts = s.split(";");
        if (parts.length != 2)
        {
            PlexLog.error("Invalid game rule format: " + s);
            return;
        }

        String gameRuleName = parts[0];
        String valueString = parts[1];

        Registry<GameRule<?>> gameRuleRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.GAME_RULE);
        GameRule<?> rule = gameRuleRegistry.get(Key.key("minecraft", gameRuleName));

        if (rule == null)
        {
            PlexLog.error(String.format("Unknown game rule: %s", gameRuleName));
            return;
        }

        if (rule.getType() == Boolean.class)
        {
            @SuppressWarnings("unchecked")
            GameRule<Boolean> boolRule = (GameRule<Boolean>) rule;
            Boolean value = Boolean.parseBoolean(valueString);
            world.setGameRule(boolRule, value);
            PlexLog.debug("Setting game rule " + gameRuleName + " for world " + world.getName() + " with value " + value);
        }
        else if (rule.getType() == Integer.class)
        {
            @SuppressWarnings("unchecked")
            GameRule<Integer> intRule = (GameRule<Integer>) rule;
            try
            {
                Integer value = Integer.parseInt(valueString);
                world.setGameRule(intRule, value);
                PlexLog.debug("Setting game rule " + gameRuleName + " for world " + world.getName() + " with value " + value);
            }
            catch (NumberFormatException e)
            {
                PlexLog.error(String.format("Invalid integer value '%s' for game rule %s", valueString, gameRuleName));
            }
        }
        else
        {
            PlexLog.error(String.format("Unknown game rule type for %s: %s", gameRuleName, rule.getType()));
        }
    }
}
