package dev.plex.util;

import dev.plex.Plex;

import java.util.Locale;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.kyori.adventure.key.Key;
import org.bukkit.GameRule;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

public class GameRuleUtil
{
    public static void apply(Plex plugin, World world)
    {
        commitGlobalGameRules(plugin, world);
        commitSpecificGameRules(plugin, world);
        PlexLog.log("Applied gamerules for world: " + world.getName().toLowerCase(Locale.ROOT));
    }

    public static void commitGlobalGameRules(Plex plugin, World world)
    {
        for (String s : plugin.config.getStringList("global_gamerules"))
        {
            readGameRules(world, s);
        }
    }

    public static void commitSpecificGameRules(Plex plugin, World world)
    {
        ConfigurationSection worlds = plugin.config.getConfigurationSection("worlds");
        if (worlds == null)
        {
            return;
        }
        worlds.getKeys(false).stream()
                .filter(key -> key.equalsIgnoreCase(world.getName()))
                .findFirst()
                .ifPresent(key -> plugin.config.getStringList("worlds." + key + ".gameRules")
                        .forEach(rule -> readGameRules(world, rule)));
    }

    private static void readGameRules(World world, String s)
    {
        String[] parts = s.split(";", 2);
        if (parts.length != 2)
        {
            PlexLog.error("Invalid game rule format: " + s);
            return;
        }

        String gameRuleName = parts[0].trim().toLowerCase(Locale.ROOT);
        String valueString = parts[1].trim();
        if (gameRuleName.isEmpty() || valueString.isEmpty())
        {
            PlexLog.error("Invalid game rule format: " + s);
            return;
        }

        Registry<GameRule<?>> gameRuleRegistry = RegistryAccess.registryAccess().getRegistry(RegistryKey.GAME_RULE);
        GameRule<?> rule;
        try
        {
            rule = gameRuleRegistry.get(Key.key("minecraft", gameRuleName));
        }
        catch (IllegalArgumentException exception)
        {
            PlexLog.error(String.format("Invalid game rule name: %s", gameRuleName));
            return;
        }

        if (rule == null)
        {
            PlexLog.error(String.format("Unknown game rule: %s", gameRuleName));
            return;
        }

        if (rule.getType() == Boolean.class)
        {
            if (!valueString.equalsIgnoreCase("true") && !valueString.equalsIgnoreCase("false"))
            {
                PlexLog.error(String.format("Invalid boolean value '%s' for game rule %s", valueString, gameRuleName));
                return;
            }
            @SuppressWarnings("unchecked")
            GameRule<Boolean> boolRule = (GameRule<Boolean>) rule;
            boolean value = Boolean.parseBoolean(valueString);
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
