package dev.plex.listener.impl;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import dev.plex.Plex;
import dev.plex.listener.ProxyListener;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class ConnectionListener extends ProxyListener
{
    public ConnectionListener(Plex plugin)
    {
        super(plugin);
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPlayerJoin(ServerConnectedEvent event)
    {
        if (event.getPreviousServer().isPresent())
        {
            plugin.server.sendMessage(message("server_switch",
                    "player", event.getPlayer().getUsername(),
                    "from", event.getPreviousServer().get().getServerInfo().getName(),
                    "to", event.getServer().getServerInfo().getName()));
        }
        else
        {
            plugin.server.sendMessage(message("server_join",
                    "player", event.getPlayer().getUsername(),
                    "server", event.getServer().getServerInfo().getName()));
        }
    }

    @Subscribe(order = PostOrder.FIRST)
    public void onPlayerLeave(DisconnectEvent event)
    {
        if (event.getPlayer().getCurrentServer().isPresent())
        {
            plugin.server.sendMessage(message("server_leave",
                    "player", event.getPlayer().getUsername(),
                    "server", event.getPlayer().getCurrentServer().get().getServerInfo().getName()));
        }
    }

    private Component message(String key, String... replacements)
    {
        String message = plugin.getMessages().getString(key, "");
        for (int i = 0; i < replacements.length; i += 2)
        {
            message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        return MiniMessage.miniMessage().deserialize(message);
    }
}
