package dev.plex.listener.impl;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import dev.plex.Plex;
import dev.plex.listener.PlexListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class ConnectionListener extends PlexListener
{
    @Subscribe(order = PostOrder.FIRST)
    public void onPlayerJoin(ServerConnectedEvent event)
    {
        if (event.getPreviousServer().isPresent())
        {
            Plex.get().server.sendMessage(miniMessage("<dark_gray>[<#ffbf00>o<dark_gray>] <yellow>"
                    + event.getPlayer().getUsername() + " switched from " + event.getPreviousServer().get().getServerInfo().getName()
                    + " to " + event.getServer().getServerInfo().getName()));
        }
        else
        {
            Plex.get().server.sendMessage(miniMessage("<dark_gray>[<green>+<dark_gray>] <yellow>"
                    + event.getPlayer().getUsername() + " joined server " + event.getServer().getServerInfo().getName()));
        }
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPlayerLeave(DisconnectEvent event)
    {
        if (event.getPlayer().getCurrentServer().isPresent())
        {
            Plex.get().server.sendMessage(miniMessage("<dark_gray>[<red>-<dark_gray>] <yellow>"
                    + event.getPlayer().getUsername() + " left server " +
                    event.getPlayer().getCurrentServer().get().getServerInfo().getName()));
        }
    }

    private Component miniMessage(String message)
    {
        return MiniMessage.miniMessage().deserialize(message);
    }
}
