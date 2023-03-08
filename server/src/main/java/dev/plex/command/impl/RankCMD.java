package dev.plex.command.impl;

import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.annotation.System;
import dev.plex.command.exception.CommandFailException;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

@CommandPermissions(level = Rank.OP, permission = "plex.rank", source = RequiredCommandSource.ANY)
@CommandParameters(name = "rank", description = "Displays your rank")
@System(value = "ranks")
public class RankCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length == 0)
        {
            if (isConsole(sender))
            {
                throw new CommandFailException("<red>When using the console, you must specify a player's rank.");
            }
            if (!(playerSender == null))
            {
                Rank rank = getPlexPlayer(playerSender).getRankFromString();
                return messageComponent("yourRank", rank.isAtLeast(Rank.ADMIN) && !getPlexPlayer(playerSender).isAdminActive() ? (playerSender.isOp() ? Rank.OP.getReadable() : Rank.NONOP.getReadable()) : rank.getReadable());
            }
        }
        else
        {
            Player player = getNonNullPlayer(args[0]);
            Rank rank = getPlexPlayer(player).getRankFromString();
            return messageComponent("otherRank", player.getName(), rank.isAtLeast(Rank.ADMIN) && !getPlexPlayer(player).isAdminActive() ? (player.isOp() ? Rank.OP.getReadable() : Rank.NONOP.getReadable()) : rank.getReadable());
        }
        return null;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        if (args.length == 1)
        {
            return PlexUtils.getPlayerNameList();
        }
        return Collections.emptyList();
    }
}