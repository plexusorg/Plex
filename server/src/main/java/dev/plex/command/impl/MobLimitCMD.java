package dev.plex.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.util.PlexUtils;

import java.util.Arrays;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MobLimitCMD extends ServerCommand
{
    public MobLimitCMD()
    {
        super(command("moblimit")
            .description("Manages the mob limit per chunk.")
            .usage("/<command> [on | off | setmax <limit>]")
            .aliases("entitylimit")
            .permission("plex.moblimit")
            .build());
    }
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context, this::status));
        command.then(literal("on")
                .executes(context -> executeCommand(context,
                        commandContext -> setEnabled(commandContext, true))));
        command.then(literal("off")
                .executes(context -> executeCommand(context,
                        commandContext -> setEnabled(commandContext, false))));
        command.then(literal("setmax")
                .then(nonNegativeInteger("limit")
                        .executes(context -> executeCommand(context,
                                commandContext -> setMaximum(commandContext, integer(context, "limit"))))));
    }

    private Component status(ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        Player playerSender = context.player();
        Chunk chunk = playerSender != null ? playerSender.getLocation().getChunk() : Bukkit.getWorlds().getFirst().getChunkAt(0, 0);
        int currentLimit = plugin.entities.getInt("entity_limit.max_mobs_per_chunk");
        int currentMobCount = (int) Arrays.stream(chunk.getEntities())
                    .filter(LivingEntity.class::isInstance)
                    .filter(entity -> !(entity instanceof Player))
                    .count();
        String status = PlexUtils.messageString(plugin.entities.getBoolean("entity_limit.mob_limit_enabled")
                    ? "mobLimitEnabled" : "mobLimitDisabled");
        return PlexUtils.messageComponent("mobLimitStatus", status, currentMobCount, currentLimit, chunk.getX(), chunk.getZ());
    }

    private Component setEnabled(ServerCommandContext context, boolean enabled)
    {
        plugin.entities.set("entity_limit.mob_limit_enabled", enabled);
        plugin.entities.save();
        return PlexUtils.messageComponent("mobLimitToggle", PlexUtils.messageString(enabled ? "stateEnabled" : "stateDisabled"));
    }

    private Component setMaximum(ServerCommandContext context, int requestedLimit)
    {
        int limitCeiling = plugin.entities.getInt("entity_limit.mob_limit_ceiling");
        int newLimit = Math.min(requestedLimit, limitCeiling);
        if (requestedLimit > limitCeiling)
        {
            context.sender().sendMessage(PlexUtils.messageComponent("mobLimitCeiling"));
        }
        plugin.entities.set("entity_limit.max_mobs_per_chunk", newLimit);
        plugin.entities.save();
        return PlexUtils.messageComponent("mobLimitSet", newLimit);
    }

}
