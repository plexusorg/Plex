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

@CommandPermissions(level = Rank.ADMIN, source = RequiredCommandSource.IN_GAME)
@CommandParameters(name = "masterbuilderworld", aliases = "mbw", description = "Teleport to the Master Builder world")
public class MasterbuilderworldCMD extends PlexCommand
{

    @Override
    public Component execute(CommandSender sender, String[] args)
    {
        // TODO: Add adminworld settings
        if (args.length == 0)
        {
            Location loc = new Location(Bukkit.getWorld("masterbuilderworld"), 0, 50, 0);
            ((Player)sender).teleportAsync(loc);
            return tl("teleportedToWorld", "Master Builder World");
        }
        return null;
    }
}
