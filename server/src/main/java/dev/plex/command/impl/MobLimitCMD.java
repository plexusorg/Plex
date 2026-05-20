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
        command.executes(context -> executeCommand(context));
        command.then(literal("on")
                .executes(context -> executeCommand(context, "on")));
        command.then(literal("off")
                .executes(context -> executeCommand(context, "off")));
        command.then(literal("setmax")
                .then(nonNegativeInteger("limit")
                        .executes(context -> executeCommand(context, "setmax", String.valueOf(integer(context, "limit"))))));
    }

    @Override
    protected Component execute(@NotNull ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        Player playerSender = context.player();
        String[] args = context.args();
        if (args.length == 0)
        {
            Chunk chunk = playerSender != null ? playerSender.getLocation().getChunk() : Bukkit.getWorlds().getFirst().getChunkAt(0, 0);

            int currentLimit = plugin.config.getInt("entity_limit.max_mobs_per_chunk");
            int currentMobCount = (int) Arrays.stream(chunk.getEntities())
                    .filter(entity -> entity instanceof LivingEntity && !(entity instanceof Player))
                    .count();

            String status = PlexUtils.messageString(plugin.config.getBoolean("entity_limit.mob_limit_enabled") ? "mobLimitEnabled" : "mobLimitDisabled");
            return PlexUtils.messageComponent("mobLimitStatus", status, currentMobCount, currentLimit, chunk.getX(), chunk.getZ());
        }

        switch (args[0].toLowerCase())
        {
            case "on":
                plugin.config.set("entity_limit.mob_limit_enabled", true);
                plugin.config.save();
                return PlexUtils.messageComponent("mobLimitToggle", PlexUtils.messageString("stateEnabled"));
            case "off":
                plugin.config.set("entity_limit.mob_limit_enabled", false);
                plugin.config.save();
                return PlexUtils.messageComponent("mobLimitToggle", PlexUtils.messageString("stateDisabled"));
            case "setmax":
                try
                {
                    if (args.length != 2)
                    {
                        return context.usage();
                    }

                    int newLimit = Integer.parseInt(args[1]);
                    if (newLimit < 0)
                    {
                        throw new NumberFormatException();
                    }

                    int limitCeiling = plugin.config.getInt("entity_limit.mob_limit_ceiling");
                    if (newLimit > limitCeiling)
                    {
                        newLimit = limitCeiling;
                        sender.sendMessage(PlexUtils.messageComponent("mobLimitCeiling"));
                    }

                    plugin.config.set("entity_limit.max_mobs_per_chunk", newLimit);
                    plugin.config.save();
                    return PlexUtils.messageComponent("mobLimitSet", newLimit);
                }
                catch (NumberFormatException e)
                {
                    return PlexUtils.messageComponent("unableToParseNumber", args[1]);
                }
            default:
                return context.usage();
        }
    }

}
