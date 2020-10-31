package me.totalfreedom.plex.command.impl;

import me.totalfreedom.plex.command.annotations.CommandParameters;
import me.totalfreedom.plex.command.annotations.CommandPermissions;
import me.totalfreedom.plex.command.PlexCommand;
import me.totalfreedom.plex.command.source.RequiredCommandSource;
import me.totalfreedom.plex.rank.enums.Rank;
import org.bukkit.command.CommandSender;

import java.util.Arrays;
import java.util.List;

@CommandPermissions(level = Rank.OP, source = RequiredCommandSource.ANY)
@CommandParameters(usage = "/<command>", aliases = "plexhelp", description = "Help with plex")
public class PlexCMD extends PlexCommand
{
    public PlexCMD() {
        super("plex");
    }

    @Override
    public void execute(CommandSender sender, String[] args)
    {
        sender.sendMessage("HI");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        return Arrays.asList("Telesphoreo", "super", "Taahh");
    }
}
