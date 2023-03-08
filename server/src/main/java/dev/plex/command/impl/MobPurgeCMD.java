package dev.plex.command.impl;

import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

@CommandPermissions(level = Rank.ADMIN, permission = "plex.mobpurge", source = RequiredCommandSource.ANY)
@CommandParameters(name = "mobpurge", description = "Purge all mobs.", usage = "/<command>", aliases = "mp")
public class MobPurgeCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, @NotNull String[] args)
    {
        HashMap<String, Integer> entityCounts = new HashMap<>();

        for (World world : Bukkit.getWorlds())
        {
            for (Entity entity : world.getEntities())
            {
                if (entity instanceof Mob)
                {
                    String type = entity.getType().name();
                    entity.remove();

                    entityCounts.put(type, entityCounts.getOrDefault(type, 0) + 1);
                }
            }
        }

        int entityCount = entityCounts.values().stream().mapToInt(a -> a).sum();

        PlexUtils.broadcast(messageComponent("removedMobs", sender.getName(), entityCount));

        /*entityCounts.forEach((entityName, numRemoved) -> {
            sender.sendMessage(messageComponent("removedEntitiesOfType", sender.getName(), numRemoved, entityName));
        });*/
        return null;
    }
}