package dev.plex.command.impl;

import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@CommandPermissions(permission = "plex.mobpurge", source = RequiredCommandSource.ANY)
@CommandParameters(name = "mobpurge", description = "Purge all mobs.", usage = "/<command> <mob>", aliases = "mp")
public class MobPurgeCMD extends PlexCommand {

    public static final List<EntityType> MOB_TYPES = new ArrayList<>();

    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, @NotNull String[] args) {
        HashMap<String, Integer> entityCounts = new HashMap<>();

        for (World world : Bukkit.getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof Mob) {
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

    // Adds a tab completion for /mp so players stop complaining we (mostly me) nuked all their mobs because a filter for some reason was never added by the REAL plex devs. Go figure. -Alco_Rs11

    public static List<String> getAllMobs() {
        List<String> mobs = new ArrayList<>();
        Arrays.stream(EntityType.values()).filter(EntityType::isAlive).filter(EntityType::isSpawnable).forEach(MOB_TYPES::add);
        for (EntityType entityType : MOB_TYPES) {
            mobs.add(entityType.name());
        }
        return mobs;
    }

    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        if (args.length == 1) {
            return getAllMobs();
        }
        return Collections.emptyList();
    }
}