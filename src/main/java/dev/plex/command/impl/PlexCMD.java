package dev.plex.command.impl;

import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.CommandSource;
import dev.plex.command.source.RequiredCommandSource;
import java.util.Arrays;
import java.util.List;
import dev.plex.command.PlexCommand;
import dev.plex.rank.enums.Rank;

@CommandPermissions(level = Rank.OP, source = RequiredCommandSource.ANY)
@CommandParameters(aliases = "plexhelp", description = "Help with plex")
public class PlexCMD extends PlexCommand
{
    public PlexCMD()
    {
        super("plex");
    }

    @Override
    public void execute(CommandSource sender, String[] args)
    {
        send("HI");
    }

    @Override
    public List<String> onTabComplete(CommandSource sender, String[] args)
    {
        return Arrays.asList("Telesphoreo", "super", "Taahh");
    }
}