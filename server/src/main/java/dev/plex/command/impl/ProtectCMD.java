package dev.plex.command.impl;

import static dev.plex.api.message.MessagePlaceholder.placeholder;

import dev.plex.util.PlexUtils;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.sk89q.worldedit.IncompleteRegionException;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.hook.WorldGuardHook;
import dev.plex.hook.WorldGuardHook.ProtectionException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Collection;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/** Creates and manages WorldGuard regions using configurable flag presets. */
public final class ProtectCMD extends ServerCommand
{
    private final WorldGuardHook worldGuard;

    public ProtectCMD(WorldGuardHook worldGuard)
    {
        super(command("protect")
                .description("Create WorldGuard regions with flag presets")
                .usage("/<command> <create | apply | remove | list | presets | reload>")
                .permission("plex.protect")
                .build());
        this.worldGuard = worldGuard;
    }

    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context, ServerCommandContext::usage));

        var create = literal("create");
        var region = word("region");
        var preset = word("preset").suggests(suggest(worldGuard::presetNames));
        preset.executes(context -> executeCommand(context, commandContext -> protect(commandContext,
                () -> create(commandContext, string(context, "region"), string(context, "preset"), null))));
        preset.then(nonNegativeInteger("radius").executes(context -> executeCommand(context, commandContext ->
                protect(commandContext, () -> create(commandContext, string(context, "region"),
                        string(context, "preset"), integer(context, "radius"))))));
        command.then(create.then(region.then(preset)));

        command.then(literal("apply").then(word("region")
                .suggests((context, builder) -> suggestMatching(builder, currentWorldRegions(context.getSource())))
                .then(word("preset").suggests(suggest(worldGuard::presetNames))
                        .executes(context -> executeCommand(context, commandContext -> protect(commandContext,
                                () -> apply(commandContext, string(context, "region"), string(context, "preset"))))))));
        command.then(literal("remove").then(word("region")
                .suggests((context, builder) -> suggestMatching(builder, currentWorldRegions(context.getSource())))
                .executes(context -> executeCommand(context, commandContext -> protect(commandContext,
                        () -> remove(commandContext, string(context, "region")))))));
        command.then(literal("list").executes(context -> executeCommand(context, this::list)));
        command.then(literal("presets").executes(context -> executeCommand(context, this::presets)));
        command.then(literal("reload").executes(context -> executeCommand(context, commandContext ->
                PlexUtils.messageComponent("protectReloaded", placeholder("count", worldGuard.reload())))));
    }

    private Component protect(ServerCommandContext context, ProtectionOperation operation)
    {
        try
        {
            return operation.execute();
        }
        catch (ProtectionException ex)
        {
            return protectionError(context, ex);
        }
        catch (IncompleteRegionException ex)
        {
            return PlexUtils.messageComponent("protectSelectionIncomplete");
        }
    }

    private Component protectionError(ServerCommandContext context, ProtectionException exception)
    {
        return switch (exception.reason())
        {
            case "region-exists" -> PlexUtils.messageComponent("protectRegionExists", placeholder("region", exception.detail()));
            case "region-not-found" -> PlexUtils.messageComponent("protectRegionNotFound", placeholder("region", exception.detail()));
            case "preset-not-found" -> PlexUtils.messageComponent("protectPresetNotFound", placeholder("preset", exception.detail()));
            case "invalid-region-id" -> PlexUtils.messageComponent("protectInvalidRegionId", placeholder("region", exception.detail()));
            case "manager-unavailable" -> PlexUtils.messageComponent("protectManagerUnavailable", placeholder("world", exception.detail()));
            case "player-only" -> PlexUtils.messageComponent("protectPlayerOnly");
            case "unsupported-region" -> PlexUtils.messageComponent("protectUnsupportedRegion", placeholder("region", exception.detail()));
            case "managed-other-world" -> PlexUtils.messageComponent("protectManagedOtherWorld", placeholder("world", exception.detail()));
            default -> PlexUtils.messageComponent("protectInvalidPreset", placeholder("error", exception.detail()));
        };
    }

    private Component create(ServerCommandContext context, String region, String preset, Integer radius) throws IncompleteRegionException
    {
        Player player = requirePlayer(context);
        if (radius == null)
        {
            worldGuard.createFromSelection(player, region, preset);
        }
        else
        {
            if (radius < 1)
            {
                return PlexUtils.messageComponent("protectInvalidRadius");
            }
            worldGuard.createAround(player, region, preset, radius);
        }
        return PlexUtils.messageComponent("protectRegionCreated", placeholder("region", region), placeholder("preset", preset), placeholder("world", player.getWorld().getName()));
    }

    private Component apply(ServerCommandContext context, String region, String preset)
    {
        Player player = requirePlayer(context);
        worldGuard.applyPreset(player.getWorld(), region, preset);
        return PlexUtils.messageComponent("protectPresetApplied", placeholder("preset", preset), placeholder("region", region));
    }

    private Component remove(ServerCommandContext context, String region)
    {
        Player player = requirePlayer(context);
        worldGuard.remove(player.getWorld(), region);
        return PlexUtils.messageComponent("protectRegionRemoved", placeholder("region", region), placeholder("world", player.getWorld().getName()));
    }

    private Component list(ServerCommandContext context)
    {
        Collection<String> names = worldGuard.managedRegionNames();
        if (names.isEmpty())
        {
            return PlexUtils.messageComponent("protectNoRegions");
        }
        context.sender().sendMessage(PlexUtils.messageComponent("protectRegionListHeader", placeholder("count", names.size())));
        for (String name : names)
        {
            ConfigurationSection region = worldGuard.config().getConfigurationSection("regions." + name);
            if (region != null)
            {
                context.sender().sendMessage(PlexUtils.messageComponent("protectRegionListEntry", placeholder("region", name), placeholder("world", region.getString("world", "?")), placeholder("preset", region.getString("preset", "custom"))));
            }
        }
        return null;
    }

    private Component presets(ServerCommandContext context)
    {
        Collection<String> names = worldGuard.presetNames();
        if (names.isEmpty())
        {
            return PlexUtils.messageComponent("protectNoPresets");
        }
        context.sender().sendMessage(PlexUtils.messageComponent("protectPresetListHeader", placeholder("count", names.size())));
        for (String name : names)
        {
            String description = worldGuard.config().getString("presets." + name + ".description", "No description");
            context.sender().sendMessage(PlexUtils.messageComponent("protectPresetListEntry", placeholder("preset", name), placeholder("description", description)));
        }
        return null;
    }

    private Player requirePlayer(ServerCommandContext context)
    {
        Player player = context.player();
        if (player == null)
        {
            throw new ProtectionException("player-only", "This operation must be run by a player");
        }
        return player;
    }

    private Collection<String> currentWorldRegions(CommandSourceStack source)
    {
        if (source.getSender() instanceof Player player)
        {
            return worldGuard.regionNames(player.getWorld());
        }
        return List.of();
    }

    @FunctionalInterface
    private interface ProtectionOperation
    {
        Component execute() throws IncompleteRegionException;
    }
}
