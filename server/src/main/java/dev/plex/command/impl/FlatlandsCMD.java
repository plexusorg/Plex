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

@CommandPermissions(level = Rank.OP, permission = "plex.flatlands", source = RequiredCommandSource.IN_GAME)
@CommandParameters(name = "flatlands", description = "Teleport to the flatlands")
public class FlatlandsCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        assert playerSender != null;
        if (args.length == 0)
        {
            Location loc = new Location(Bukkit.getWorld("flatlands"), 0, 50, 0);
            playerSender.teleportAsync(loc);
            return messageComponent("teleportedToWorld", "flatlands");
        }
        return null;
    }
}
