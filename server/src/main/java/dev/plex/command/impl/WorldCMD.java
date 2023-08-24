package dev.plex.command.impl;

import com.google.common.collect.Lists;
import dev.plex.Plex;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.rank.enums.Rank;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

@CommandPermissions(level = Rank.OP, permission = "plex.world", source = RequiredCommandSource.IN_GAME)
@CommandParameters(name = "world", description = "Teleport to a world.", usage = "/<command> <world>")
public class WorldCMD extends PlexCommand
{
    private static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        assert playerSender != null;
        if (args.length != 1)
        {
            return usage();
        }

        World world = getNonNullWorld(args[0]);
        boolean playerWorld = args[0].matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        if (playerWorld && Plex.get().getModuleManager().getModules().stream().anyMatch(plexModule -> plexModule.getPlexModuleFile().getName().equalsIgnoreCase("Module-TFMExtras")))
        {
            checkRank(playerSender, Rank.ADMIN, "plex.world.playerworlds");
        }
        playerSender.teleportAsync(world.getSpawnLocation());
        return messageComponent("playerWorldTeleport", world.getName());
    }


    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        final List<String> completions = Lists.newArrayList();
        final Player player = (Player) sender;
        if (args.length == 1)
        {
            @NotNull List<World> worlds = Bukkit.getWorlds();
            for (World world : worlds)
            {
                String worldName = world.getName();

                try
                {
                    final UUID uuid = UUID.fromString(worldName);
                    if (uuid.equals(player.getUniqueId()) || silentCheckRank(player, Rank.ADMIN, "plex.world.playerworlds"))
                    {
                        completions.add(worldName);
                    }
                }
                catch (Exception e)
                {
                    completions.add(worldName);
                }
            }
        }

        return completions;
    }
}
