package dev.plex.command.impl;

import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.rank.enums.Rank;

import java.util.List;

// TODO: See ranks of other players

@CommandPermissions(level = Rank.OP, source = RequiredCommandSource.IN_GAME)
@CommandParameters(description = "Displays your rank")
public class RankCMD extends PlexCommand {
    public RankCMD() {
        super("rank");
    }

    @Override
    public Component execute(CommandSender sender, String[] args) {
        send(tl("yourRank", sender.getPlexPlayer().getRank()));
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return null;
    }
}