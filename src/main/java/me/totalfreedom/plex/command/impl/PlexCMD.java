package me.totalfreedom.plex.command.impl;

import me.totalfreedom.plex.command.PlexCommand;
import me.totalfreedom.plex.command.annotation.CommandParameters;
import me.totalfreedom.plex.command.annotation.CommandPermissions;
import me.totalfreedom.plex.command.source.CommandSource;
import me.totalfreedom.plex.command.source.RequiredCommandSource;
import me.totalfreedom.plex.rank.enums.Rank;

import java.util.Arrays;
import java.util.List;

@CommandPermissions(level = Rank.OP, source = RequiredCommandSource.ANY)
@CommandParameters(aliases = "plexhelp", description = "Help with plex")
public class PlexCMD extends PlexCommand
{
    public PlexCMD() {
        super("plex");
    }

    @Override
    public void execute(CommandSource sender, String[] args)
    {
        send("HI");
    }

    @Override
    public List<String> onTabComplete(CommandSource sender, String[] args) {
        return Arrays.asList("Telesphoreo", "super", "Taahh");
    }
}
