package dev.plex.listener.impl;

import dev.plex.Plex;

import dev.plex.hook.VaultHook;
import dev.plex.listener.ServerListenerBase;
import dev.plex.meta.PlayerMeta;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerJoinEvent;

public class TabListener extends ServerListenerBase
{
    public TabListener(Plex plugin)
    {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event)
    {
        Player player = event.getPlayer();
        PlexPlayer plexPlayer = plugin.getPlayerService().getPlayer(player.getUniqueId());
        player.playerListName(PlexUtils.mmDeserialize(PlayerMeta.getColor(plugin.config, plexPlayer) + player.getName()));
    }
}
