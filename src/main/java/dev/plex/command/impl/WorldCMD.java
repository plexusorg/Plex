package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.CommandArgumentException;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.rank.enums.Rank;
import java.util.List;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandPermissions(level = Rank.OP, permission = "plex.world", source = RequiredCommandSource.IN_GAME)
@CommandParameters(name = "world", description = "Teleport to a world.", usage = "/<command> <world>")
public class WorldCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length != 1)
        {
            throw new CommandArgumentException();
        }
        World world = getNonNullWorld(args[0]);
        playerSender.teleportAsync(new Location(world, 0, world.getHighestBlockYAt(0, 0) + 1, 0, 0, 0));
        return tl("playerWorldTeleport", world.getName());
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        if (args.length == 1)
        {
            return Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList());
        }
        return ImmutableList.of();
    }
}
