package dev.plex.command.impl;

import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.CommandSource;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.rank.enums.Rank;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.Collections;
import java.util.List;

@CommandPermissions(level = Rank.ADMIN, source = RequiredCommandSource.IN_GAME)
@CommandParameters(aliases = "aw", description = "Teleport to the adminworld")
public class AdminworldCMD extends PlexCommand
{
    public AdminworldCMD()
    {
        super("adminworld");
    }

    @Override
    public void execute(CommandSource sender, String[] args)
    {
        // TODO: Add adminworld settings
        if (args.length == 0)
        {
            Location loc = new Location(Bukkit.getWorld("adminworld"), 0, 50, 0);
            sender.getPlayer().teleportAsync(loc);
            send(tl("teleportedToWorld", "adminworld"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSource sender, String[] args)
    {
        return Collections.emptyList();
    }
}
