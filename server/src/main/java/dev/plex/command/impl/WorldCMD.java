package dev.plex.command.impl;

import com.google.common.collect.Lists;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.RequiredCommandSource;

import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandPermissions(permission = "plex.world", source = RequiredCommandSource.IN_GAME)
@CommandParameters(name = "world", description = "Teleport to a world.", usage = "/<command> <world>")
public class WorldCMD extends ServerCommand
{
    private static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context));
        command.then(word("world")
                .suggests((context, builder) ->
                {
                    if (!(context.getSource().getSender() instanceof Player player))
                    {
                        return builder.buildFuture();
                    }
                    List<String> completions = Lists.newArrayList();
                    for (World world : Bukkit.getWorlds())
                    {
                        String worldName = world.getName();
                        try
                        {
                            UUID uuid = UUID.fromString(worldName);
                            if (uuid.equals(player.getUniqueId()) || silentCheckPermission(player, "plex.world.playerworlds"))
                            {
                                completions.add(worldName);
                            }
                        }
                        catch (Exception e)
                        {
                            completions.add(worldName);
                        }
                    }
                    return suggestMatching(builder, completions);
                })
                .executes(context -> executeCommand(context, string(context, "world"))));
    }

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
        if (playerWorld && plugin.getModuleManager().getModules().stream().anyMatch(plexModule -> plexModule.getPlexModuleFile().getName().equalsIgnoreCase("Module-TFMExtras")))
        {
            checkPermission(playerSender, "plex.world.playerworlds");
        }
        playerSender.teleportAsync(world.getSpawnLocation());
        return messageComponent("playerWorldTeleport", world.getName());
    }

}
