package dev.plex.command.impl;

import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

@CommandPermissions(permission = "plex.entitywipe", source = RequiredCommandSource.ANY)
@CommandParameters(name = "entitywipe", description = "Remove various server entities that may cause lag, such as dropped items, minecarts, and boats.", usage = "/<command> [entity] [radius]", aliases = "ew,rd")
public class EntityWipeCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, @NotNull String[] args)
    {
        List<String> entityBlacklist = plugin.config.getStringList("entitywipe_list");

        List<String> entityWhitelist = new LinkedList<>(Arrays.asList(args));

        boolean radiusSpecified = !entityWhitelist.isEmpty() && isNumeric(entityWhitelist.get(entityWhitelist.size() - 1)); // try and detect if the last argument of the command is a number
        boolean useBlacklist = args.length == 0 || (args.length == 1 && radiusSpecified); // if there are no arguments or the one argument is a number
        int radius = 0;

        PlexLog.debug("using blacklist: " + useBlacklist);
        PlexLog.debug("radius specified: " + radiusSpecified);

        if (radiusSpecified)
        {
            radius = parseInt(sender, args[entityWhitelist.size() - 1]); // get the args length as the size of the list
            radius *= radius;
            entityWhitelist.remove(entityWhitelist.size() - 1); // remove the radius from the list
        }

        PlexLog.debug("radius: " + radius);

        EntityType[] entityTypes = EntityType.values();
        entityWhitelist.removeIf(name ->
        {
            boolean res = Arrays.stream(entityTypes).noneMatch(entityType -> name.equalsIgnoreCase(entityType.name()));
            if (res)
            {
                sender.sendMessage(messageComponent("invalidEntityType", name));
            }
            return res;
        });

        HashMap<String, Integer> entityCounts = new HashMap<>();

        for (World world : Bukkit.getWorlds())
        {
            for (Entity entity : world.getEntities())
            {
                if (entity.getType() != EntityType.PLAYER)
                {
                    String type = entity.getType().name();

                    if (useBlacklist ? entityBlacklist.stream().noneMatch(entityName -> entityName.equalsIgnoreCase(type)) : entityWhitelist.stream().anyMatch(entityName -> entityName.equalsIgnoreCase(type)))
                    {
                        if (radius > 0)
                        {
                            PlexLog.debug("we got here, radius is > 0");
                            if (playerSender != null && entity.getWorld() == playerSender.getWorld() && playerSender.getLocation().distanceSquared(entity.getLocation()) > radius)
                            {
                                PlexLog.debug("continuing");
                                continue;
                            }
                        }
                        PlexLog.debug("removed entity: " + entity.getType().name());
                        entity.remove();

                        entityCounts.put(type, entityCounts.getOrDefault(type, 0) + 1);
                    }
                }
            }
        }

        int entityCount = entityCounts.values().stream().mapToInt(a -> a).sum();

        if (useBlacklist)
        {
            PlexUtils.broadcast(messageComponent("removedEntities", sender.getName(), entityCount));
        }
        else
        {
            if (entityCount == 0)
            {
                sender.sendMessage(messageComponent("noRemovedEntities"));
                return null;
            }
            String list = String.join(", ", entityCounts.keySet());
            list = list.replaceAll("(, )(?!.*\1)", (list.indexOf(", ") == list.lastIndexOf(", ") ? "" : ",") + " and ");
            PlexUtils.broadcast(messageComponent("removedEntitiesOfTypes", sender.getName(), entityCount, list));
        }
        return null;
    }

    public @NotNull List<String> smartTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        if (silentCheckPermission(sender, this.getPermission()))
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
            return entities.stream().toList();
        }
        return Collections.emptyList();
    }

    private Integer parseInt(CommandSender sender, String string)
    {
        try
        {
            return Integer.parseInt(string);
        }
        catch (NumberFormatException ex)
        {
            sender.sendMessage(mmString("<red>" + string + "<red> is not a valid number!"));
        }
        return null;
    }

    private boolean isNumeric(String string)
    {
        if (string == null)
        {
            return false;
        }
        try
        {
            int num = Integer.parseInt(string);
        }
        catch (NumberFormatException nfe)
        {
            return false;
        }
        return true;
    }
}