package dev.plex.listener.impl;

import dev.plex.listener.PlexListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.block.data.Openable;
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

import java.util.Arrays;
import java.util.List;

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

    /**
     * Check if a Player has the ability to modify the world they are in
     *
     * @param player      The player who wants to modify the world
     * @param showMessage Whether the message from the config.yml should be shown
     * @return Returns true if the person has the ability to modify the world
     */
    private boolean canModifyWorld(Player player, boolean showMessage)
    {
        World world = player.getWorld();
        String permission = plugin.config.getString("worlds." + world.getName().toLowerCase() + ".modification.permission");
        if (permission == null)
        {
            return true;
        }
        if (player.hasPermission(permission))
        {
            return true;
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
        String permission = plugin.config.getString("worlds." + destination.getName().toLowerCase() + ".entry.permission");
        if (permission == null)
        {
            return true;
        }
        if (player.hasPermission(permission))
        {
            return true;
        }

        String noEntry = plugin.config.getString("worlds." + destination.getName().toLowerCase() + ".entry.message");
        if (noEntry != null)
        {
            player.sendMessage(MiniMessage.miniMessage().deserialize(noEntry));
        }
        return false;
    }
}