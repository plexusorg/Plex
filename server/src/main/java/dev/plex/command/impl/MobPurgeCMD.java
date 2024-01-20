package dev.plex.command.impl;

import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.text.WordUtils;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@CommandPermissions(permission = "plex.mobpurge", source = RequiredCommandSource.ANY)
@CommandParameters(name = "mobpurge", description = "Purge all mobs.", usage = "/<command> [mob]", aliases = "mp")
public class MobPurgeCMD extends PlexCommand
{
    private final List<EntityType> MOB_TYPES = new ArrayList<>();

    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, @NotNull String[] args)
    {
        EntityType type = null;
        String mobName = null;
        if (args.length > 0)
        {
            try
            {
                type = EntityType.valueOf(args[0].toUpperCase());
            }
            catch (Exception e)
            {
                PlexLog.debug("A genius tried and failed removing the following invalid mob: " + args[0].toUpperCase());
                send(sender, messageComponent("notAValidMob"));
                return null;
            }
            if (!MOB_TYPES.contains(type))
            {
                PlexLog.debug(Arrays.deepToString(MOB_TYPES.toArray()));
                PlexLog.debug("A genius tried to remove a mob that doesn't exist: " + args[0].toUpperCase());
                sender.sendMessage(messageComponent("notAValidMobButValidEntity"));
                return null;
            }
        }
        if (type != null)
        {
            mobName = WordUtils.capitalizeFully(type.name().replace("_", " "));
            PlexLog.debug("The args aren't null so the mob is: " + mobName);
        }
        int count = purgeMobs(type);
        if (type != null)
        {
            PlexUtils.broadcast(messageComponent("removedEntitiesOfTypes", sender.getName(), count, mobName));
            PlexLog.debug("All " + count + " of " + mobName + " were removed");
        }
        else
        {
            PlexUtils.broadcast(messageComponent("removedMobs", sender.getName(), count));
            PlexLog.debug("All " + count + " valid mobs were removed");
        }
        sender.sendMessage(messageComponent("amountOfMobsRemoved", count, (type != null ? mobName : "mob") + multipleS(count)));
        return null;
    }

    private String multipleS(int count)
    {
        return (count == 1 ? "" : "s");
    }

    private int purgeMobs(EntityType type)
    {
        int removed = 0;
        for (World world : Bukkit.getWorlds())
        {
            for (Entity entity : world.getLivingEntities())
            {
                if (entity instanceof LivingEntity && !(entity instanceof Player))
                {
                    if (type != null && !entity.getType().equals(type))
                    {
                        continue;
                    }
                    entity.remove();
                    removed++;
                }
            }
        }
        return removed;
    }

    private List<String> getAllMobs()
    {
        List<String> mobs = new ArrayList<>();
        Arrays.stream(EntityType.values()).filter(EntityType::isAlive).filter(EntityType::isSpawnable).forEach(MOB_TYPES::add);
        for (EntityType entityType : MOB_TYPES)
        {
            mobs.add(entityType.name());
        }
        return mobs;
    }

    public @NotNull List<String> smartTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        if (args.length == 1 && silentCheckPermission(sender, this.getPermission()))
        {
            return getAllMobs();
        }
        return Collections.emptyList();
    }
}