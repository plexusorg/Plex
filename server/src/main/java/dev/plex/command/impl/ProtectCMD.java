package dev.plex.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.sk89q.worldedit.IncompleteRegionException;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.hook.WorldGuardHook;
import dev.plex.hook.WorldGuardHook.ProtectionException;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
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
        command.executes(context -> executeCommand(context));

        command.then(literal("create")
                .then(word("region")
                        .then(word("preset")
                                .suggests(suggest(worldGuard::presetNames))
                                .executes(context -> executeCommand(context, "create", string(context, "region"), string(context, "preset")))
                                .then(nonNegativeInteger("radius")
                                        .executes(context -> executeCommand(context, "create", string(context, "region"),
                                                string(context, "preset"), Integer.toString(integer(context, "radius"))))))));

        command.then(literal("apply")
                .then(word("region")
                        .suggests((context, builder) -> suggestMatching(builder, currentWorldRegions(context.getSource())))
                        .then(word("preset")
                                .suggests(suggest(worldGuard::presetNames))
                                .executes(context -> executeCommand(context, "apply", string(context, "region"), string(context, "preset"))))));

        command.then(literal("remove")
                .then(word("region")
                        .suggests((context, builder) -> suggestMatching(builder, currentWorldRegions(context.getSource())))
                        .executes(context -> executeCommand(context, "remove", string(context, "region")))));
        command.then(literal("list").executes(context -> executeCommand(context, "list")));
        command.then(literal("presets").executes(context -> executeCommand(context, "presets")));
        command.then(literal("reload").executes(context -> executeCommand(context, "reload")));
    }

    @Override
    protected Component execute(@NotNull ServerCommandContext context)
    {
        String[] args = context.args();
        if (args.length == 0)
        {
            return context.usage();
        }

        try
        {
            return switch (args[0].toLowerCase(Locale.ROOT))
            {
                case "create" -> create(context, args);
                case "apply" -> apply(context, args);
                case "remove" -> remove(context, args);
                case "list" -> list(context);
                case "presets" -> presets(context);
                case "reload" -> context.messageComponent("protectReloaded", worldGuard.reload());
                default -> context.usage();
            };
        }
        catch (ProtectionException ex)
        {
            return switch (ex.reason())
            {
                case "region-exists" -> context.messageComponent("protectRegionExists", ex.detail());
                case "region-not-found" -> context.messageComponent("protectRegionNotFound", ex.detail());
                case "preset-not-found" -> context.messageComponent("protectPresetNotFound", ex.detail());
                case "invalid-region-id" -> context.messageComponent("protectInvalidRegionId", ex.detail());
                case "manager-unavailable" -> context.messageComponent("protectManagerUnavailable", ex.detail());
                case "player-only" -> context.messageComponent("protectPlayerOnly");
                case "unsupported-region" -> context.messageComponent("protectUnsupportedRegion", ex.detail());
                case "managed-other-world" -> context.messageComponent("protectManagedOtherWorld", ex.detail());
                default -> context.messageComponent("protectInvalidPreset", ex.detail());
            };
        }
        catch (IncompleteRegionException ex)
        {
            return context.messageComponent("protectSelectionIncomplete");
        }
    }

    private Component create(ServerCommandContext context, String[] args) throws IncompleteRegionException
    {
        if (args.length < 3)
        {
            return context.usage("/protect create <region> <preset> [radius]");
        }
        Player player = requirePlayer(context);
        if (args.length == 3)
        {
            worldGuard.createFromSelection(player, args[1], args[2]);
        }
        else
        {
            int radius = Integer.parseInt(args[3]);
            if (radius < 1)
            {
                return context.messageComponent("protectInvalidRadius");
            }
            worldGuard.createAround(player, args[1], args[2], radius);
        }
        return context.messageComponent("protectRegionCreated", args[1], args[2], player.getWorld().getName());
    }

    private Component apply(ServerCommandContext context, String[] args)
    {
        if (args.length != 3)
        {
            return context.usage("/protect apply <region> <preset>");
        }
        Player player = requirePlayer(context);
        worldGuard.applyPreset(player.getWorld(), args[1], args[2]);
        return context.messageComponent("protectPresetApplied", args[2], args[1]);
    }

    private Component remove(ServerCommandContext context, String[] args)
    {
        if (args.length != 2)
        {
            return context.usage("/protect remove <region>");
        }
        Player player = requirePlayer(context);
        worldGuard.remove(player.getWorld(), args[1]);
        return context.messageComponent("protectRegionRemoved", args[1], player.getWorld().getName());
    }

    private Component list(ServerCommandContext context)
    {
        Collection<String> names = worldGuard.managedRegionNames();
        if (names.isEmpty())
        {
            return context.messageComponent("protectNoRegions");
        }
        context.send(context.sender(), context.messageComponent("protectRegionListHeader", names.size()));
        for (String name : names)
        {
            ConfigurationSection region = worldGuard.config().getConfigurationSection("regions." + name);
            if (region != null)
            {
                context.send(context.sender(), context.messageComponent("protectRegionListEntry", name,
                        region.getString("world", "?"), region.getString("preset", "custom")));
            }
        }
        return null;
    }

    private Component presets(ServerCommandContext context)
    {
        Collection<String> names = worldGuard.presetNames();
        if (names.isEmpty())
        {
            return context.messageComponent("protectNoPresets");
        }
        context.send(context.sender(), context.messageComponent("protectPresetListHeader", names.size()));
        for (String name : names)
        {
            String description = worldGuard.config().getString("presets." + name + ".description", "No description");
            context.send(context.sender(), context.messageComponent("protectPresetListEntry", name, description));
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
}
