package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.Plex;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@CommandParameters(name = "debug", description = "Debug command", usage = "/<command> <redis-reset> [player]")
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
            if (Plex.get().getRedisConnection().getJedis().exists(player.getUniqueId().toString()))
            {
                Plex.get().getRedisConnection().getJedis().del(player.getUniqueId().toString());
                return componentFromString("Successfully reset " + player.getName() + "'s redis punishments!").color(NamedTextColor.YELLOW);
            }
            return componentFromString("Couldn't find player in redis punishments.");
        }


        return null;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        return args.length == 1 ? PlexUtils.getPlayerNameList() : ImmutableList.of();
    }
}