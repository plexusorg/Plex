package dev.plex.command.impl;

import com.google.common.primitives.Ints;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.util.PlexUtils;
import dev.plex.util.EntityRemovalUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class EntityWipeCMD extends ServerCommand
{
    public EntityWipeCMD()
    {
        super(command("entitywipe")
            .description("Remove various server entities that may cause lag, such as dropped items, minecarts, and boats.")
            .usage("/<command> [entity] [radius]")
            .aliases("ew,rd")
            .permission("plex.entitywipe")
            .build());
    }
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context, commandContext ->
                wipe(commandContext, new WipeRequest(List.of(), 0))));
        command.then(greedyString("entities")
                .suggests(suggestGreedyWords(() ->
                {
                    return Arrays.stream(EntityType.values()).filter(type -> type != EntityType.PLAYER)
                            .map(EntityType::name).toList();
                }))
                .executes(context -> executeCommand(context, commandContext ->
                        wipe(commandContext, parseRequest(normalizeGreedyString(string(context, "entities")))))));
    }

    private WipeRequest parseRequest(String entities)
    {
        List<String> arguments = Arrays.asList(entities.split(" "));
        Integer parsedRadius = Ints.tryParse(arguments.getLast());
        boolean radiusSpecified = parsedRadius != null && parsedRadius != Integer.MIN_VALUE;
        int radius = radiusSpecified ? parsedRadius : 0;
        int entityArgumentCount = arguments.size() - Boolean.compare(radiusSpecified, false);
        return new WipeRequest(new LinkedList<>(arguments.subList(0, entityArgumentCount)), radius);
    }

    private Component wipe(ServerCommandContext context, WipeRequest request)
    {
        CommandSender sender = context.sender();
        Player playerSender = context.player();
        List<String> entityBlacklist = plugin.entities.getStringList("entitywipe_list");
        int radius = request.radius();
        List<String> entityWhitelist = request.entityTypes();

        boolean useBlacklist = entityWhitelist.isEmpty();
        Collection<String> selectedTypes = selectedEntityTypes(context, sender,
                useBlacklist ? entityBlacklist : entityWhitelist, useBlacklist);
        Player radiusCenter = Optional.ofNullable(playerSender).filter(player -> radius != 0).orElse(null);
        int range = Math.abs(radius);
        org.bukkit.Location center = radiusCenter == null ? null : radiusCenter.getLocation().clone();
        double rangeSquared = (double)range * range;
        EntityRemovalUtil.removeLoaded(plugin, entity -> selected(entity, selectedTypes, useBlacklist)
                        && (center == null || entity.getWorld().equals(center.getWorld())
                        && entity.getLocation().distanceSquared(center) <= rangeSquared))
                .thenAccept(counts -> reportRemoval(context, sender, useBlacklist, counts));
        return null;
    }

    private boolean selected(Entity entity, Collection<String> selectedTypes, boolean useBlacklist)
    {
        return entity.getType() != EntityType.PLAYER
                && selectedTypes.contains(entity.getType().name()) != useBlacklist;
    }

    private void reportRemoval(ServerCommandContext context, CommandSender sender, boolean useBlacklist,
                               java.util.Map<String, Integer> entityCounts)
    {
        int entityCount = entityCounts.values().stream().mapToInt(a -> a).sum();

        if (useBlacklist)
        {
            PlexUtils.broadcast(PlexUtils.messageComponent("removedEntities", context.senderName(), entityCount));
        }
        else
        {
            if (entityCount == 0)
            {
                sender.sendMessage(PlexUtils.messageComponent("noRemovedEntities"));
                return;
            }
            String list = String.join(", ", entityCounts.keySet());
            list = list.replaceAll("(, )(?!.*\1)", (list.indexOf(", ") == list.lastIndexOf(", ") ? "" : ",") + " and ");
            PlexUtils.broadcast(PlexUtils.messageComponent("removedEntitiesOfTypes", context.senderName(), entityCount, list));
        }
    }

    private Collection<String> selectedEntityTypes(ServerCommandContext context, CommandSender sender,
                                                   List<String> names, boolean configuredBlacklist)
    {
        if (configuredBlacklist)
        {
            return names.stream().map(name -> name.toUpperCase(Locale.ROOT)).collect(Collectors.toSet());
        }
        Collection<String> types = new java.util.HashSet<>();
        for (String name : names)
        {
            try
            {
                types.add(EntityType.valueOf(name.toUpperCase(Locale.ROOT)).name());
            }
            catch (IllegalArgumentException ignored)
            {
                sender.sendMessage(PlexUtils.messageComponent("invalidEntityType", name));
            }
        }
        return types;
    }

    private record WipeRequest(List<String> entityTypes, int radius) { }
}
