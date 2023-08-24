package dev.plex.listener.impl;

import com.destroystokyo.paper.event.entity.PlayerNaturallySpawnCreaturesEvent;
import dev.plex.Plex;
import dev.plex.listener.PlexListener;
import dev.plex.player.PlexPlayer;
import dev.plex.rank.enums.Rank;
import dev.plex.rank.enums.Title;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.data.Openable;
import org.bukkit.block.data.Powerable;
import org.bukkit.block.data.type.Door;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.command.Command;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.*;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class WorldListener extends PlexListener
{
    private final List<String> EDIT_COMMANDS = Arrays.asList("bigtree", "ebigtree", "largetree", "elargetree", "break", "ebreak", "antioch", "nuke", "editsign", "tree", "etree");

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockPlace(BlockPlaceEvent event)
    {
        if (!canModifyWorld(event.getPlayer(), true))
        {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockBreak(BlockBreakEvent event)
    {
        if (!canModifyWorld(event.getPlayer(), true))
        {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteractWorld(PlayerInteractEvent event)
    {
        if (event.getInteractionPoint() != null && event.getInteractionPoint().getBlock().getBlockData() instanceof Openable) return;
        if (!canModifyWorld(event.getPlayer(), true))
        {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteractWorld(PlayerInteractEntityEvent event)
    {
        if (!canModifyWorld(event.getPlayer(), true))
        {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteractWorld(PlayerItemDamageEvent event)
    {
        if (!canModifyWorld(event.getPlayer(), true))
        {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteractWorld(EntityDamageByEntityEvent event)
    {
        if (!(event.getDamager() instanceof Player player)) return;
        if (!canModifyWorld(player, true))
        {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntitySpawn(EntitySpawnEvent event)
    {
        if (event.getEntityType() != EntityType.SLIME)
        {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event)
    {
        // If the person has permission to modify the world, we don't need to block WorldEdit
        if (canModifyWorld(event.getPlayer(), false))
        {
            return;
        }

        String message = event.getMessage();
        // Don't check the arguments
        message = message.replaceAll("\\s.*", "").replaceFirst("/", "");
        Command command = Bukkit.getCommandMap().getCommand(message);
        if (command != null)
        {
            // This does check for aliases
            boolean isWeCommand = command instanceof PluginIdentifiableCommand && ((PluginIdentifiableCommand) command).getPlugin().equals(Bukkit.getPluginManager().getPlugin("WorldEdit"));
            boolean isFaweCommand = command instanceof PluginIdentifiableCommand && ((PluginIdentifiableCommand) command).getPlugin().equals(Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit"));
            if (isWeCommand || isFaweCommand || EDIT_COMMANDS.contains(message.toLowerCase()))
            {
                event.getPlayer().sendMessage(Component.text("You do not have permission to use that command in this world.").color(NamedTextColor.RED));
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onWorldTeleport(PlayerTeleportEvent event)
    {
        if (!canEnterWorld(event.getPlayer(), event.getTo().getWorld()))
        {
            event.setCancelled(true);
        }
    }

    private boolean checkLevel(PlexPlayer player, String[] requiredList)
    {
        PlexLog.debug("Checking world required levels " + Arrays.toString(requiredList));
        boolean hasAccess = false;
        for (String required : requiredList)
        {
            PlexLog.debug("Checking if player has " + required);
            if (required.startsWith("Title"))
            {
                String titleString = required.split("\\.")[1];
                Title title = Title.valueOf(titleString.toUpperCase(Locale.ROOT));
                switch (title)
                {
                    case DEV ->
                    {
                        hasAccess = PlexUtils.DEVELOPERS.contains(player.getUuid().toString());
                    }
                    case MASTER_BUILDER ->
                    {
                        hasAccess = Plex.get().config.contains("titles.masterbuilders") && Plex.get().config.getStringList("titles.masterbuilders").contains(player.getName());
                    }
                    case OWNER ->
                    {
                        hasAccess = Plex.get().config.contains("titles.owners") && Plex.get().config.getStringList("titles.owners").contains(player.getName());
                    }
                    default ->
                    {
                        return false;
                    }
                }
            }
            else if (required.startsWith("Rank"))
            {
                String rankString = required.split("\\.")[1];
                Rank rank = Rank.valueOf(rankString.toUpperCase(Locale.ROOT));
                hasAccess = rank.isAtLeast(Rank.ADMIN) ? player.isAdminActive() && player.getRankFromString().isAtLeast(rank) : player.getRankFromString().isAtLeast(rank);
            }
        }
        return hasAccess;
    }

    /**
     * Check if a Player has the ability to modify the world they are in
     *
     * @param player      The player who wants to modify the world
     * @param showMessage Whether the message from the config.yml should be shown
     * @return Returns true if the person has the ability to modify the world
     */
    private boolean canModifyWorld(Player player, boolean showMessage)
    {
        PlexPlayer plexPlayer = plugin.getPlayerCache().getPlexPlayerMap().get(player.getUniqueId());
        World world = player.getWorld();
        if (plugin.getSystem().equalsIgnoreCase("permissions"))
        {
            String permission = plugin.config.getString("worlds." + world.getName().toLowerCase() + ".modification.permission");
            if (permission == null)
            {
                return true;
            }
            if (player.hasPermission(permission))
            {
                return true;
            }
        }
        else if (plugin.getSystem().equalsIgnoreCase("ranks"))
        {
            if (plugin.config.contains("worlds." + world.getName().toLowerCase() + ".modification.requiredLevels"))
            {
                @NotNull List<String> requiredLevel = plugin.config.getStringList("worlds." + world.getName().toLowerCase() + ".modification.requiredLevels");
                if (checkLevel(plexPlayer, requiredLevel.toArray(String[]::new)))
                {
                    return true;
                }
            }
            else
            {
                return true;
            }
        }

        if (showMessage)
        {
            String noEdit = plugin.config.getString("worlds." + world.getName().toLowerCase() + ".modification.message");
            if (noEdit != null)
            {
                player.sendMessage(MiniMessage.miniMessage().deserialize(noEdit));
            }
        }
        return false;
    }

    /**
     * Check if a Player has the ability to enter the requested world
     *
     * @param player The player who wants to enter the world
     * @return Returns true if the person has the ability to enter the world
     */
    private boolean canEnterWorld(Player player, World destination)
    {
        PlexPlayer plexPlayer = plugin.getPlayerCache().getPlexPlayerMap().get(player.getUniqueId());
        if (plugin.getSystem().equalsIgnoreCase("permissions"))
        {
            String permission = plugin.config.getString("worlds." + destination.getName().toLowerCase() + ".entry.permission");
            if (permission == null)
            {
                return true;
            }
            if (player.hasPermission(permission))
            {
                return true;
            }
        }
        else if (plugin.getSystem().equalsIgnoreCase("ranks"))
        {
            if (plugin.config.contains("worlds." + destination.getName().toLowerCase() + ".entry.requiredLevels"))
            {
                @NotNull List<String> requiredLevel = plugin.config.getStringList("worlds." + destination.getName().toLowerCase() + ".entry.requiredLevels");
                if (checkLevel(plexPlayer, requiredLevel.toArray(String[]::new)))
                {
                    return true;
                }
            }
            else
            {
                return true;
            }
        }

        String noEntry = plugin.config.getString("worlds." + destination.getName().toLowerCase() + ".entry.message");
        if (noEntry != null)
        {
            player.sendMessage(MiniMessage.miniMessage().deserialize(noEntry));
        }
        return false;
    }
}