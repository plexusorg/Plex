package dev.plex.listener.impl;

import dev.plex.listener.PlexListener;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

public class LoginListener extends PlexListener
{
    private final String banMessage = plugin.config.getString("banning.message");

    @EventHandler
    public void onPreLogin(AsyncPlayerPreLoginEvent event)
    {
        if (plugin.getBanManager().isBanned(event.getUniqueId()))
        {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED,
                    LegacyComponentSerializer.legacyAmpersand().deserialize(banMessage));
        }
    }
}
