package dev.plex.command.impl;

import org.bukkit.Bukkit;

import dev.plex.util.PlexUtils;
import com.google.common.collect.Lists;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.command.source.RequiredCommandSource;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class WorldCMD extends ServerCommand
{
    public WorldCMD()
    {
        super(command("world")
            .description("Teleport to a world.")
            .usage("/<command> <world>")
            .permission("plex.world")
            .source(RequiredCommandSource.IN_GAME)
            .build());
    }
    private static final Pattern UUID_PATTERN = Pattern.compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context, ServerCommandContext::usage));
        command.then(word("world")
                .suggests((context, builder) ->
                {
                    if (!(context.getSource().getSender() instanceof Player player))
                    {
                        return builder.buildFuture();
                    }
                    UUID playerId = player.getUniqueId();
                    boolean canViewPlayerWorlds = player.hasPermission("plex.world.playerworlds");
                    CompletableFuture<com.mojang.brigadier.suggestion.Suggestions> suggestions = new CompletableFuture<>();
                    Bukkit.getGlobalRegionScheduler().run(plugin, task ->
                    {
                        List<String> completions = Lists.newArrayList();
                        for (World world : Bukkit.getWorlds())
                        {
                            String worldName = world.getName();
                            try
                            {
                                UUID uuid = UUID.fromString(worldName);
                                if (uuid.equals(playerId) || canViewPlayerWorlds) completions.add(worldName);
                            }
                            catch (IllegalArgumentException ignored)
                            {
                                completions.add(worldName);
                            }
                        }
                        suggestMatching(builder, completions).whenComplete((result, failure) ->
                        {
                            if (failure == null) suggestions.complete(result);
                            else suggestions.completeExceptionally(failure);
                        });
                    });
                    return suggestions;
                })
                .executes(context -> executeCommand(context,
                        commandContext -> executeTyped(commandContext, string(context, "world")))));
    }

    private Component executeTyped(ServerCommandContext context, String worldName)
    {
        CommandSender sender = context.sender();
        Player playerSender = context.player();
        assert playerSender != null;
        boolean canVisitPlayerWorlds = playerSender.hasPermission("plex.world.playerworlds");
        Bukkit.getGlobalRegionScheduler().run(plugin, task ->
        {
            World world = Bukkit.getWorld(worldName);
            if (world == null)
            {
                sender.sendMessage(PlexUtils.messageComponent("worldNotFound"));
                return;
            }
            boolean playerWorld = UUID_PATTERN.matcher(worldName).matches();
            boolean playerWorldsEnabled = plugin.getModuleManager().getModules().stream()
                    .anyMatch(module -> module.getPlexModuleFile().getName().equalsIgnoreCase("Module-TFMExtras"));
            if (playerWorld && playerWorldsEnabled && !canVisitPlayerWorlds)
            {
                sender.sendMessage(PlexUtils.messageComponent("noPermissionNode", "plex.world.playerworlds"));
                return;
            }
            Location spawn = world.getSpawnLocation().clone();
            playerSender.getScheduler().run(plugin, entityTask ->
            {
                playerSender.teleportAsync(spawn).whenComplete((teleported, failure) ->
                {
                    if (failure != null || !teleported)
                    {
                        playerSender.sendMessage(Component.text("Unable to teleport to that world."));
                        return;
                    }
                    playerSender.sendMessage(PlexUtils.messageComponent("playerWorldTeleport", world.getName()));
                });
            }, () -> sender.sendMessage(Component.text("Unable to teleport because the player disconnected.")));
        });
        return null;
    }

}
