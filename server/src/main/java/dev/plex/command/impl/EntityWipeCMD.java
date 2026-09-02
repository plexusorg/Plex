package dev.plex.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.util.PlexUtils;

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
        command.executes(context -> executeCommand(context));
        command.then(greedyString("entities")
                .suggests(suggestGreedyWords(() ->
                {
                    List<String> entities = new ArrayList<>();
                    for (World world : Bukkit.getWorlds())
                    {
                        for (Entity entity : world.getEntities())
                        {
                            if (entity.getType() != EntityType.PLAYER)
                            {
                                entities.add(entity.getType().name());
                            }
                        }
                    }
                    return entities;
                }))
                .executes(context -> executeCommand(context, argsWithGreedy(string(context, "entities")))));
    }

    @Override
    protected Component execute(@NotNull ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        Player playerSender = context.player();
        String[] args = context.args();
        List<String> entityBlacklist = plugin.entities.getStringList("entitywipe_list");

        String lastArgument = Arrays.stream(args).reduce((first, second) -> second).orElse("");
        boolean radiusSpecified = org.apache.commons.lang3.math.NumberUtils.isParsable(lastArgument);
        int radius = org.apache.commons.lang3.math.NumberUtils.toInt(lastArgument);
        int entityArgumentCount = args.length - Boolean.compare(radiusSpecified, false);
        List<String> entityWhitelist = new LinkedList<>(Arrays.asList(args).subList(0, entityArgumentCount));

        boolean useBlacklist = entityWhitelist.isEmpty();
        Collection<String> selectedTypes = (useBlacklist ? entityBlacklist : entityWhitelist).stream()
                .map(name -> name.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
        Player radiusCenter = Optional.ofNullable(playerSender).filter(player -> radius != 0).orElse(null);
        int range = Math.abs(radius);
        Collection<Entity> entities = radiusCenter != null
                ? radiusCenter.getWorld().getNearbyEntities(radiusCenter.getLocation(), range, range, range,
                        entity -> radiusCenter.getLocation().distanceSquared(entity.getLocation()) <= range * range)
                : Bukkit.getWorlds().stream().flatMap(world -> world.getEntities().stream()).toList();
        HashMap<String, Integer> entityCounts = new HashMap<>();

        for (Entity entity : entities)
        {
            if (entity.getType() == EntityType.PLAYER)
            {
                continue;
            }
            String type = entity.getType().name();
            if (selectedTypes.contains(type) == useBlacklist)
            {
                continue;
            }
            entity.remove();
            entityCounts.merge(type, 1, Integer::sum);
        }

        int entityCount = entityCounts.values().stream().mapToInt(a -> a).sum();

        if (useBlacklist)
        {
            PlexUtils.broadcast(context.messageComponent("removedEntities", context.senderName(), entityCount));
        }
        else
        {
            if (entityCount == 0)
            {
                sender.sendMessage(context.messageComponent("noRemovedEntities"));
                return null;
            }
            String list = String.join(", ", entityCounts.keySet());
            list = list.replaceAll("(, )(?!.*\1)", (list.indexOf(", ") == list.lastIndexOf(", ") ? "" : ",") + " and ");
            PlexUtils.broadcast(context.messageComponent("removedEntitiesOfTypes", context.senderName(), entityCount, list));
        }
        return null;
    }
}
