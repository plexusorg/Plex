package dev.plex.listener.impl;

import dev.plex.Plex;
import dev.plex.listener.ServerListenerBase;
import dev.plex.player.PlexPlayer;
import dev.plex.util.CommandUtils;
import dev.plex.util.PlexUtils;
import io.papermc.paper.event.player.AsyncChatEvent;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class MuteListener extends ServerListenerBase
{
    public MuteListener(Plex plugin)
    {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event)
    {
        PlexPlayer plexPlayer = plugin.getPlayerService().cachedPlayer(event.getPlayer().getUniqueId());
        if (plexPlayer != null && plexPlayer.isMuted())
        {
            event.getPlayer().sendMessage(PlexUtils.messageComponent("muted"));
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent event)
    {
        PlexPlayer plexPlayer = plugin.getPlayerService().cachedPlayer(event.getPlayer().getUniqueId());
        if (plexPlayer != null && plexPlayer.isMuted())
        {
            if (CommandUtils.matchesCommand(plugin, event.getMessage(), plugin.config.getStringList("block_on_mute")))
            {
                event.getPlayer().sendMessage(PlexUtils.messageComponent("muted"));
                event.setCancelled(true);
            }
        }
    }

}
