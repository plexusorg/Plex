package dev.plex.listener.impl;

import dev.plex.Plex;
import dev.plex.cache.DataUtils;
import dev.plex.cache.player.PlayerCache;
import dev.plex.listener.PlexListener;
import dev.plex.player.PlexPlayer;
import dev.plex.rank.enums.Rank;
import dev.plex.rank.enums.Title;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.jetbrains.annotations.NotNull;

public class WorldListener extends PlexListener
{
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e)
    {
        Player player = e.getPlayer();
        PlexPlayer plexPlayer = PlayerCache.getPlexPlayerMap().get(player.getUniqueId());
        World world = player.getWorld();
        if (plugin.getSystem().equalsIgnoreCase("permissions"))
        {
            String permission = plugin.config.getString("worlds." + world.getName().toLowerCase() + ".permission");
            if (permission == null)
            {
                return;
            }
            if (player.hasPermission(permission))
            {
                return;
            }
        }
        else if (plugin.getSystem().equalsIgnoreCase("ranks"))
        {
            if (plugin.config.contains("worlds." + world.getName().toLowerCase() + ".requiredLevels"))
            {
                @NotNull List<String> requiredLevel = plugin.config.getStringList("worlds." + world.getName().toLowerCase() + ".requiredLevels");
                if (checkLevel(plexPlayer, requiredLevel.toArray(String[]::new)))
                {
                    return;
                }
            }
            else
            {
                return;
            }
        }

        e.setCancelled(true);
        String noEdit = plugin.config.getString("worlds." + world.getName().toLowerCase() + ".noEdit");
        if (noEdit != null)
        {
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(noEdit));
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e)
    {
        Player player = e.getPlayer();
        PlexPlayer plexPlayer = PlayerCache.getPlexPlayerMap().get(player.getUniqueId());
        World world = player.getWorld();
        if (plugin.getSystem().equalsIgnoreCase("permissions"))
        {
            String permission = plugin.config.getString("worlds." + world.getName().toLowerCase() + ".permission");
            if (permission == null)
            {
                return;
            }
            if (player.hasPermission(permission))
            {
                return;
            }
        }
        else if (plugin.getSystem().equalsIgnoreCase("ranks"))
        {
            if (plugin.config.contains("worlds." + world.getName().toLowerCase() + ".requiredLevels"))
            {
                @NotNull List<String> requiredLevel = plugin.config.getStringList("worlds." + world.getName().toLowerCase() + ".requiredLevels");
                if (checkLevel(plexPlayer, requiredLevel.toArray(String[]::new)))
                {
                    return;
                }
            }
            else
            {
                return;
            }
        }

        e.setCancelled(true);
        String noEdit = plugin.config.getString("worlds." + world.getName().toLowerCase() + ".noEdit");
        if (noEdit != null)
        {
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(noEdit));
        }
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent e)
    {
        if (e.getEntityType() != EntityType.SLIME)
        {
            return;
        }
        e.setCancelled(true);
    }

    // TODO: Add an entry setting in the config.yml and allow checking for all worlds
    @EventHandler
    public void onWorldTeleport(PlayerTeleportEvent e)
    {
        final World adminworld = Bukkit.getWorld("adminworld");
        if (adminworld == null)
        {
            return;
        }
        PlexPlayer plexPlayer = DataUtils.getPlayer(e.getPlayer().getUniqueId());
        if (e.getTo().getWorld().equals(adminworld))
        {
            if (plugin.getSystem().equals("ranks") && !plexPlayer.isAdminActive())
            {
                e.setCancelled(true);
            }
            else if (plugin.getSystem().equals("permissions") && !e.getPlayer().hasPermission("plex.enter.adminworld"))
            {
                e.setCancelled(true);
            }
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
                    case DEV -> {
                        hasAccess = PlexUtils.DEVELOPERS.contains(player.getUuid().toString());
                    }
                    case MASTER_BUILDER -> {
                        hasAccess = Plex.get().config.contains("titles.masterbuilders") && Plex.get().config.getStringList("titles.masterbuilders").contains(player.getName());
                    }
                    case OWNER -> {
                        hasAccess = Plex.get().config.contains("titles.owners") && Plex.get().config.getStringList("titles.owners").contains(player.getName());
                    }
                    default -> {
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
}