package dev.plex.command.impl;

import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.rank.enums.Rank;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandPermissions(level = Rank.OP, permission = "plex.rank", source = RequiredCommandSource.IN_GAME)
@CommandParameters(name = "rank", description = "Displays your rank")
public class RankCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length == 0)
        {
            if (!(playerSender == null))
            {
                Rank rank = getPlexPlayer(playerSender).getRankFromString();
                return messageComponent("yourRank", rank.getReadable());
            }
        }
        else
        {
            Player player = getNonNullPlayer(args[0]);
            Rank rank = getPlexPlayer(player).getRankFromString();
            return messageComponent("otherRank", player.getName(), rank.getReadable());
        }
        return null;
    }
}