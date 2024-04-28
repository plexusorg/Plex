package dev.plex.listener.impl;

import de.myzelyam.api.vanish.PlayerShowEvent;
import dev.plex.cache.DataUtils;
import dev.plex.listener.PlexListener;
import dev.plex.meta.PlayerMeta;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

public class VanishListener extends PlexListener
{
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerUnvanish(PlayerShowEvent event)
    {
        if (!PlexUtils.hasVanishPlugin())
        {
            return;
        }
        if (event.isSilent())
        {
            return;
        }
        PlexPlayer plexPlayer = DataUtils.getPlayer(event.getPlayer().getUniqueId());
        String loginMessage = PlayerMeta.getLoginMessage(plexPlayer);
        if (!loginMessage.isEmpty())
        {
            PlexUtils.broadcast(PlexUtils.stringToComponent(loginMessage));
        }
    }
}
