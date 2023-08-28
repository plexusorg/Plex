package dev.plex.listener.impl;

import dev.plex.cache.DataUtils;
import dev.plex.listener.PlexListener;
import dev.plex.meta.PlayerMeta;
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
        player.playerListName(Component.text(player.getName()).color(PlayerMeta.getColor(plexPlayer)));
    }
}
