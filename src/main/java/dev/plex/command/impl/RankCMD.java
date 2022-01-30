package dev.plex.command.impl;

import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.rank.enums.Rank;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

// TODO: See ranks of other players

@CommandPermissions(level = Rank.OP, permission = "plex.rank", source = RequiredCommandSource.IN_GAME)
@CommandParameters(name = "rank", description = "Displays your rank")
public class RankCMD extends PlexCommand
{
    @Override
    public Component execute(CommandSender sender, String[] args)
    {
        return tl("yourRank", getPlexPlayer((Player)sender).getRank());
    }
}