package dev.plex.listener.impl;

import dev.plex.cache.DataUtils;
import dev.plex.event.AdminAddEvent;
import dev.plex.event.AdminRemoveEvent;
import dev.plex.event.AdminSetRankEvent;
import dev.plex.listener.PlexListener;
import dev.plex.player.PlexPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;

public class TabListener extends PlexListener
{
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        PlexPlayer plexPlayer = DataUtils.getPlayer(player.getUniqueId());
        player.playerListName(Component.text(player.getName()).color(plugin.getRankManager().getColor(plexPlayer)));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAdminAdd(AdminAddEvent event)
    {
        PlexPlayer plexPlayer = event.getPlexPlayer();
        Player player = event.getPlexPlayer().getPlayer();
        if (player == null)
        {
            return;
        }
        player.playerListName(Component.text(player.getName()).color(plugin.getRankManager().getColor(plexPlayer)));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAdminRemove(AdminRemoveEvent event)
    {
        PlexPlayer plexPlayer = event.getPlexPlayer();
        Player player = event.getPlexPlayer().getPlayer();
        if (player == null)
        {
            return;
        }
        player.playerListName(Component.text(player.getName()).color(plugin.getRankManager().getColor(plexPlayer)));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAdminSetRank(AdminSetRankEvent event)
    {
        PlexPlayer plexPlayer = event.getPlexPlayer();
        Player player = event.getPlexPlayer().getPlayer();
        if (player == null)
        {
            return;
        }
        player.playerListName(Component.text(player.getName()).color(plugin.getRankManager().getColor(plexPlayer)));
    }
}
