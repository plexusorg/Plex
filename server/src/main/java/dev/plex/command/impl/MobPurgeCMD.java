package dev.plex.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.papermc.paper.command.brigadier.CommandSourceStack;
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

public class MobPurgeCMD extends ServerCommand
{
    public MobPurgeCMD()
    {
        super(command("mobpurge")
            .description("Purge all mobs.")
            .usage("/<command> [mob]")
            .aliases("mp")
            .permission("plex.mobpurge")
            .build());
    }
    private final List<EntityType> MOB_TYPES = new ArrayList<>();

    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context));
        command.then(word("mob")
                .suggests(suggest(this::getAllMobs))
                .executes(context -> executeCommand(context, string(context, "mob"))));
    }

    @Override
    protected Component execute(@NotNull ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        Player playerSender = context.player();
        String[] args = context.args();
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
                context.send(sender, context.messageComponent("notAValidMob"));
                return null;
            }
            if (!MOB_TYPES.contains(type))
            {
                PlexLog.debug(Arrays.deepToString(MOB_TYPES.toArray()));
                PlexLog.debug("A genius tried to remove a mob that doesn't exist: " + args[0].toUpperCase());
                sender.sendMessage(context.messageComponent("notAValidMobButValidEntity"));
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
            PlexUtils.broadcast(context.messageComponent("removedEntitiesOfTypes", sender.getName(), count, mobName));
            PlexLog.debug("All " + count + " of " + mobName + " were removed");
        }
        else
        {
            PlexUtils.broadcast(context.messageComponent("removedMobs", sender.getName(), count));
            PlexLog.debug("All " + count + " valid mobs were removed");
        }
        sender.sendMessage(context.messageComponent("amountOfMobsRemoved", count, type != null ? mobName + multipleS(count) : context.messageString(count == 1 ? "mobSingular" : "mobPlural")));
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

    }
