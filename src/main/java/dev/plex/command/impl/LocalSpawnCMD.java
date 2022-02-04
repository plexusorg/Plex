package dev.plex.command.impl;

import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.rank.enums.Rank;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@CommandParameters(name = "localspawn", description = "Teleport to the spawnpoint of the world you are in")
@CommandPermissions(level = Rank.OP, permission = "plex.spawnpoint")
public class LocalSpawnCMD extends PlexCommand
{
    @Override
    protected Component execute(CommandSender sender, Player playerSender, String[] args)
    {
        playerSender.teleportAsync(playerSender.getWorld().getSpawnLocation());
        return tl("teleportedToWorldSpawn");
    }
}