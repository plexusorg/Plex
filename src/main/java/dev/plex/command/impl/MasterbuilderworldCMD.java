package dev.plex.command.impl;

import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.rank.enums.Rank;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.Collections;
import java.util.List;

@CommandPermissions(level = Rank.ADMIN, source = RequiredCommandSource.IN_GAME)
@CommandParameters(aliases = "mbw", description = "Teleport to the Master Builder world")
public class MasterbuilderworldCMD extends PlexCommand
{
    public MasterbuilderworldCMD()
    {
        super("masterbuilderworld");
    }

    @Override
    public Component execute(CommandSender sender, String[] args)
    {
        // TODO: Add adminworld settings
        if (args.length == 0)
        {
            Location loc = new Location(Bukkit.getWorld("masterbuilderworld"), 0, 50, 0);
            sender.getPlayer().teleportAsync(loc);
            send(tl("teleportedToWorld", "Master Builder world"));
        }
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String[] args)
    {
        return Collections.emptyList();
    }
}
