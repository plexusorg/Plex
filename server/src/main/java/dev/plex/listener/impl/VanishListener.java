package dev.plex.listener.impl;

import dev.plex.Plex;

import de.myzelyam.api.vanish.PostPlayerShowEvent;
import dev.plex.listener.ServerListenerBase;
import dev.plex.meta.PlayerMeta;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

public class VanishListener extends ServerListenerBase
{
    public VanishListener(Plex plugin)
    {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerUnvanish(PostPlayerShowEvent event)
    {
        if (event.isSilent())
        {
            return;
        }

        PlexPlayer plexPlayer = plugin.getPlayerService().getPlayer(event.getPlayer().getUniqueId());
        if (plexPlayer == null)
        {
            PlexLog.warn("Unable to load Plex player data for {0}; skipping reappear messages.", event.getPlayer().getName());
            return;
        }

        String loginMessage = PlayerMeta.getLoginMessage(plexPlayer);
        if (!loginMessage.isEmpty())
        {
            PlexUtils.broadcast(PlexUtils.stringToComponent(loginMessage));
            PlexUtils.broadcast(PlexUtils.messageComponent("loginMessage", plexPlayer.getName()));
        }
    }
}
