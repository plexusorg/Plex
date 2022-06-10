package dev.plex.command.impl;

import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.rank.enums.Rank;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandParameters(name = "localspawn", description = "Teleport to the spawnpoint of the world you are in")
@CommandPermissions(level = Rank.OP, permission = "plex.localspawn", source = RequiredCommandSource.IN_GAME)
public class LocalSpawnCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        assert playerSender != null;
        playerSender.teleportAsync(playerSender.getWorld().getSpawnLocation());
        return messageComponent("teleportedToWorldSpawn");
    }
}