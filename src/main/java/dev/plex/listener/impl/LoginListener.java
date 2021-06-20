package dev.plex.listener.impl;

import dev.plex.listener.PlexListener;
import dev.plex.util.PlexLog;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class LoginListener extends PlexListener
{
    //TODO: Customizable MSGS

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event)
    {
        PlexLog.log(String.valueOf(plugin.getBanManager().isBanned(event.getUniqueId())));
        if (plugin.getBanManager().isBanned(event.getUniqueId()))
        {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, "§cYou're currently banned from this server.\n§cPlease appeal at §6https://forum.telesphoreo.me");
        }
    }

}
