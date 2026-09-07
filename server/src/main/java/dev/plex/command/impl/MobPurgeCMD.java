package dev.plex.command.impl;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.util.PlexLog;
import dev.plex.api.message.ActionBroadcast;
import dev.plex.util.CapturedActionBroadcast;
import dev.plex.util.PlexUtils;
import dev.plex.util.EntityRemovalUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
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
    private static final List<EntityType> MOB_TYPES = Arrays.stream(EntityType.values())
            .filter(EntityType::isAlive).filter(EntityType::isSpawnable).toList();

    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context,
                commandContext -> executeTyped(commandContext, null)));
        command.then(word("mob")
                .suggests(suggest(this::getAllMobs))
                .executes(context -> executeCommand(context,
                        commandContext -> executeTyped(commandContext, string(context, "mob")))));
    }

    private Component executeTyped(ServerCommandContext context, String requestedMob)
    {
        CommandSender sender = context.sender();
        Player playerSender = context.player();
        EntityType type = null;
        String mobName = null;
        if (requestedMob != null)
        {
            try
            {
                type = EntityType.valueOf(requestedMob.toUpperCase());
            }
            catch (Exception e)
            {
                PlexLog.debug("A genius tried and failed removing the following invalid mob: " + requestedMob.toUpperCase());
                sender.sendMessage(PlexUtils.messageComponent("notAValidMob"));
                return null;
            }
            if (!MOB_TYPES.contains(type))
            {
                PlexLog.debug(Arrays.deepToString(MOB_TYPES.toArray()));
                PlexLog.debug("A genius tried to remove a mob that doesn't exist: " + requestedMob.toUpperCase());
                sender.sendMessage(PlexUtils.messageComponent("notAValidMobButValidEntity"));
                return null;
            }
        }
        if (type != null)
        {
            mobName = Arrays.stream(type.name().split("_"))
                    .map(word -> word.substring(0, 1) + word.substring(1).toLowerCase(Locale.ROOT))
                    .collect(Collectors.joining(" "));
            PlexLog.debug("The args aren't null so the mob is: " + mobName);
        }
        EntityType selectedType = type;
        String selectedName = mobName;
        ActionBroadcast broadcast = CapturedActionBroadcast.capture(sender);
        EntityRemovalUtil.removeLoaded(plugin, entity -> entity instanceof LivingEntity
                && !(entity instanceof Player) && (selectedType == null || entity.getType() == selectedType))
                .thenAccept(counts -> report(context, sender, broadcast, selectedType, selectedName,
                        counts.values().stream().mapToInt(Integer::intValue).sum()));
        return null;
    }

    private void report(ServerCommandContext context, CommandSender sender, ActionBroadcast broadcast, EntityType type, String mobName, int count)
    {
        if (type != null)
        {
            broadcast.send(PlexUtils.messageComponent("removedEntitiesOfTypes", Placeholder.parsed("sender", context.senderName()), Placeholder.unparsed("count", String.valueOf(count)), Placeholder.parsed("types", mobName)));
            PlexLog.debug("All " + count + " of " + mobName + " were removed");
        }
        else
        {
            broadcast.send(PlexUtils.messageComponent("removedMobs", Placeholder.parsed("sender", context.senderName()), Placeholder.unparsed("count", String.valueOf(count))));
            PlexLog.debug("All " + count + " valid mobs were removed");
        }
        sender.sendMessage(PlexUtils.messageComponent("amountOfMobsRemoved", Placeholder.unparsed("count", String.valueOf(count)), Placeholder.parsed("mobs", type != null ? mobName + multipleS(count) : PlexUtils.messageString(count == 1 ? "mobSingular" : "mobPlural"))));
    }

    private String multipleS(int count)
    {
        return (count == 1 ? "" : "s");
    }

    private List<String> getAllMobs()
    {
        List<String> mobs = new ArrayList<>();
        for (EntityType entityType : MOB_TYPES)
        {
            mobs.add(entityType.name());
        }
        return mobs;
    }

    }
