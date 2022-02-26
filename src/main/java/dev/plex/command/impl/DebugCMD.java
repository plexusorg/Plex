package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandParameters(name = "debug", description = "Debug command", usage = "/<command> <redis-reset | gamerules> [player]")
@CommandPermissions(level = Rank.EXECUTIVE, permission = "plex.debug")
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
            Player player = getNonNullPlayer(args[1]);
            if (plugin.getRedisConnection().getJedis().exists(player.getUniqueId().toString()))
            {
                plugin.getRedisConnection().getJedis().del(player.getUniqueId().toString());
                return componentFromString("Successfully reset " + player.getName() + "'s Redis punishments!").color(NamedTextColor.YELLOW);
            }
            return componentFromString("Couldn't find player in Redis punishments.");
        }
        if (args[0].equalsIgnoreCase("gamerules"))
        {
            for (World world : Bukkit.getWorlds())
            {
                PlexUtils.commitGameRules(world);
                PlexLog.debug("Set gamerules for world: " + world.getName());
            }
            return mmString("<aqua>Re-applied game all the game rules!");
        }
        return null;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        return args.length == 1 ? PlexUtils.getPlayerNameList() : ImmutableList.of();
    }
}