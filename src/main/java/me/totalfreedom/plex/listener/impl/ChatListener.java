package me.totalfreedom.plex.listener.impl;

import me.totalfreedom.plex.Plex;
import me.totalfreedom.plex.cache.PlayerCache;
import me.totalfreedom.plex.listener.PlexListener;
import me.totalfreedom.plex.player.PlexPlayer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener extends PlexListener
{

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event)
    {
        PlexPlayer plexPlayer = PlayerCache.getPlexPlayerMap().get(event.getPlayer().getUniqueId());
        if (!plexPlayer.getPrefix().isEmpty())
        {
            event.setFormat(String.format("%s %s §7» %s", plexPlayer.getPrefix(), ChatColor.RESET + plexPlayer.getName(), event.getMessage()));
        } else if (Plex.get().getRankManager().isAdmin(plexPlayer))
        {
            event.setFormat(String.format("%s %s §7» %s", plexPlayer.getRankFromString().getPrefix(), ChatColor.RESET + plexPlayer.getName(), event.getMessage()));
        }
    }

}
