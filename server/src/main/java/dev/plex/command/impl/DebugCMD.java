package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.annotation.System;
import dev.plex.rank.enums.Rank;
import dev.plex.util.GameRuleUtil;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@CommandParameters(name = "pdebug", description = "Plex's debug command", usage = "/<command> <aliases <command> | redis-reset <player> | gamerules>")
@CommandPermissions(level = Rank.EXECUTIVE, permission = "plex.debug")
@System(debug = true)
public class DebugCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length == 0)
        {
            return usage();
        }
        if (args[0].equalsIgnoreCase("redis-reset"))
        {
            if (args.length == 2)
            {
                Player player = getNonNullPlayer(args[1]);
                if (plugin.getRedisConnection().getJedis().exists(player.getUniqueId().toString()))
                {
                    plugin.getRedisConnection().getJedis().del(player.getUniqueId().toString());
                    return componentFromString("Successfully reset " + player.getName() + "'s Redis punishments!").color(NamedTextColor.YELLOW);
                }
                return componentFromString("Couldn't find player in Redis punishments.");
            }
        }
        if (args[0].equalsIgnoreCase("gamerules"))
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
            return mmString("<aqua>Re-applied game all the game rules!");
        }
        if (args[0].equalsIgnoreCase("aliases"))
        {
            if (args.length == 2)
            {
                String commandName = args[1];
                Command command = plugin.getServer().getCommandMap().getCommand(commandName);
                if (command == null)
                {
                    return mmString("<red>That command could not be found!");
                }
                return mmString("<aqua>Aliases for " + commandName + " are: " + Arrays.toString(command.getAliases().toArray(new String[0])));
            }
        }
        return usage();
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        return args.length == 1 ? PlexUtils.getPlayerNameList() : ImmutableList.of();
    }
}