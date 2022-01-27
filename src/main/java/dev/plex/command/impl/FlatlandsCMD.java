package dev.plex.command.impl;

import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.rank.enums.Rank;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.List;

@CommandPermissions(level = Rank.ADMIN, source = RequiredCommandSource.IN_GAME)
@CommandParameters(name = "flatlands", description = "Teleport to the flatlands")
public class FlatlandsCMD extends PlexCommand
{

    @Override
    public Component execute(CommandSender sender, String[] args)
    {
        if (args.length == 0)
        {
            Location loc = new Location(Bukkit.getWorld("flatlands"), 0, 50, 0);
            ((Player)sender).teleportAsync(loc);
            return tl("teleportedToWorld", "flatlands");
        }
        return null;
    }
}
