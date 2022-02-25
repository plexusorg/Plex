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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandPermissions(level = Rank.ADMIN, permission = "plex.adminworld", source = RequiredCommandSource.IN_GAME)
@CommandParameters(name = "adminworld", aliases = "aw", description = "Teleport to the adminworld")
public class AdminworldCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        assert playerSender != null;
        // TODO: Add adminworld settings
        if (args.length == 0)
        {
            Location loc = new Location(Bukkit.getWorld("adminworld"), 0, 50, 0);
            playerSender.teleportAsync(loc);
            return messageComponent("teleportedToWorld", "adminworld");
        }
        return null;
    }
}
