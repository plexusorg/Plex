package dev.plex.listener.impl;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.DisconnectEvent;
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
        if (event.previousServer() != null)
        {
            Plex.get().server.sendMessage(miniMessage("<dark_gray>[<#ffbf00>o<dark_gray>] <yellow>"
                    + event.player().username() + " switched from " + event.previousServer().serverInfo().name()
                    + " to " + event.target().serverInfo().name()));
        }
        else
        {
            Plex.get().server.sendMessage(miniMessage("<dark_gray>[<green>+<dark_gray>] <yellow>"
                    + event.player().username() + " joined server " + event.target().serverInfo().name()));
        }
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPlayerLeave(DisconnectEvent event)
    {
        if (event.player().connectedServer() != null)
        {
            Plex.get().server.sendMessage(miniMessage("<dark_gray>[<red>-<dark_gray>] <yellow>"
                    + event.player().username() + " left server " +
                    event.player().connectedServer().serverInfo().name()));
        }
    }

    private Component miniMessage(String message)
    {
        return MiniMessage.miniMessage().deserialize(message);
    }
}
