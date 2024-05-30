package dev.plex.command.impl;

import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@CommandParameters(name = "moblimit", usage = "/<command> [on/off/setmax <limit>]", aliases = "entitylimit", description = "Manages the mob limit per chunk.")
@CommandPermissions(permission = "plex.moblimit", source = RequiredCommandSource.ANY)
public class MobLimitCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length == 0)
        {
            Chunk chunk = playerSender != null ? playerSender.getLocation().getChunk() : Bukkit.getWorlds().get(0).getChunkAt(0, 0);

            int currentLimit = plugin.config.getInt("entity_limit.max_mobs_per_chunk");
            int currentMobCount = (int) Arrays.stream(chunk.getEntities())
                    .filter(entity -> entity instanceof LivingEntity && !(entity instanceof Player))
                    .count();

            String status = plugin.config.getBoolean("entity_limit.mob_limit_enabled") ? "<green>Enabled" : "<red>Disabled";
            String chunkCoords = String.format("<gray>(<em><white>Chunk<gray>: <reset>%d, %d<gray>)", chunk.getX(), chunk.getZ());

            return PlexUtils.messageComponent("mobLimitStatus", status, currentMobCount, currentLimit, chunkCoords);
        }

        switch (args[0].toLowerCase())
        {
            case "on":
                plugin.config.set("entity_limit.mob_limit_enabled", true);
                plugin.config.save();
                return PlexUtils.messageComponent("mobLimitToggle", "enabled");
            case "off":
                plugin.config.set("entity_limit.mob_limit_enabled", false);
                plugin.config.save();
                return PlexUtils.messageComponent("mobLimitToggle", "disabled");
            case "setmax":
                try
                {
                    if (args.length != 2) return usage();

                    int newLimit = Integer.parseInt(args[1]);
                    if (newLimit < 0) throw new NumberFormatException();

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
                    return PlexUtils.messageComponent("invalidMobLimit");
                }
            default:
                return usage();
        }
    }

    @Override
    public @NotNull List<String> smartTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        if (silentCheckPermission(sender, this.getPermission()))
        {
            if (args.length == 1)
            {
                return Arrays.asList("on", "off", "setmax");
            }
            if (args.length == 2)
            {
                return Collections.singletonList("<limit>");
            }
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }
}