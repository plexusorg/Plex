package dev.plex.listener.impl;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import dev.plex.Plex;
import dev.plex.listener.ProxyListener;
import dev.plex.network.VanishBridgeMessage;
import dev.plex.util.PlexLog;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class ConnectionListener extends ProxyListener
{
    private static final MinecraftChannelIdentifier VANISH_CHANNEL = MinecraftChannelIdentifier.from(VanishBridgeMessage.CHANNEL);
    private final Map<UUID, PendingConnection> pendingConnections = new ConcurrentHashMap<>();
    private final Set<UUID> hiddenPlayers = ConcurrentHashMap.newKeySet();

    public ConnectionListener(Plex plugin)
    {
        super(plugin);
        plugin.getServer().getChannelRegistrar().register(VANISH_CHANNEL);
    }

    @Subscribe(priority = Short.MAX_VALUE - 1)
    public void onPlayerJoin(ServerConnectedEvent event)
    {
        UUID playerId = event.getPlayer().getUniqueId();
        PendingConnection pending = new PendingConnection(
                event.getPreviousServer().map(connection -> connection.getServerInfo().getName()).orElse(null),
                event.getServer().getServerInfo().getName());
        pendingConnections.put(playerId, pending);

        if (!event.getServer().sendPluginMessage(VANISH_CHANNEL, VanishBridgeMessage.query(playerId).encode()))
        {
            pendingConnections.remove(playerId, pending);
            if (!hiddenPlayers.contains(playerId))
            {
                announceConnection(event.getPlayer(), pending);
            }
        }
    }

    @Subscribe
    public void onPluginMessage(PluginMessageEvent event)
    {
        if (!VANISH_CHANNEL.equals(event.getIdentifier()))
        {
            return;
        }
        event.setResult(PluginMessageEvent.ForwardResult.handled());

        if (!(event.getSource() instanceof ServerConnection backend))
        {
            return;
        }

        try
        {
            VanishBridgeMessage message = VanishBridgeMessage.decode(event.getData());
            Optional<Player> connectedPlayer = plugin.getServer().getPlayer(message.playerId());
            if (connectedPlayer.flatMap(Player::getCurrentServer)
                    .filter(connection -> connection.getServerInfo().getName().equals(backend.getServerInfo().getName()))
                    .isEmpty())
            {
                return;
            }
            Player player = connectedPlayer.orElseThrow();

            switch (message.action())
            {
                case VISIBLE -> handleVisibilityResponse(player, false);
                case HIDDEN -> handleVisibilityResponse(player, true);
                case HIDE -> handleHide(player, backend, message.silent());
                case SHOW -> handleShow(player, backend, message.silent());
                case QUERY -> PlexLog.warn("Ignoring a vanish state query sent by backend server {0}.", backend.getServerInfo().getName());
            }
        }
        catch (IOException ex)
        {
            PlexLog.warn("Ignoring invalid vanish message from backend server {0}: {1}", backend.getServerInfo().getName(), ex.getMessage());
        }
    }

    @Subscribe(priority = Short.MAX_VALUE - 1)
    public void onPlayerLeave(DisconnectEvent event)
    {
        UUID playerId = event.getPlayer().getUniqueId();
        pendingConnections.remove(playerId);
        boolean hidden = hiddenPlayers.remove(playerId);
        if (event.getPlayer().getCurrentServer().isPresent())
        {
            if (!hidden)
            {
                broadcast("server_leave",
                        "player", event.getPlayer().getUsername(),
                        "server", event.getPlayer().getCurrentServer().get().getServerInfo().getName());
            }
        }
    }

    private void handleVisibilityResponse(Player player, boolean hidden)
    {
        UUID playerId = player.getUniqueId();
        if (hidden)
        {
            hiddenPlayers.add(playerId);
        }
        else
        {
            hiddenPlayers.remove(playerId);
        }

        PendingConnection pending = pendingConnections.remove(playerId);
        if (!hidden && pending != null)
        {
            announceConnection(player, pending);
        }
    }

    private void handleHide(Player player, ServerConnection backend, boolean silent)
    {
        hiddenPlayers.add(player.getUniqueId());
        if (!silent)
        {
            broadcast("server_leave",
                    "player", player.getUsername(),
                    "server", backend.getServerInfo().getName());
        }
    }

    private void handleShow(Player player, ServerConnection backend, boolean silent)
    {
        hiddenPlayers.remove(player.getUniqueId());
        if (!silent)
        {
            broadcast("server_join",
                    "player", player.getUsername(),
                    "server", backend.getServerInfo().getName());
        }
    }

    private void announceConnection(Player player, PendingConnection pending)
    {
        if (pending.previousServer() == null)
        {
            broadcast("server_join",
                    "player", player.getUsername(),
                    "server", pending.currentServer());
        }
        else
        {
            broadcast("server_switch",
                    "player", player.getUsername(),
                    "from", pending.previousServer(),
                    "to", pending.currentServer());
        }
    }

    private void broadcast(String key, String... replacements)
    {
        String message = plugin.getMessages().getString(key, "");
        if (message.isBlank())
        {
            return;
        }
        for (int i = 0; i < replacements.length; i += 2)
        {
            message = message.replace("{" + replacements[i] + "}", replacements[i + 1]);
        }
        plugin.server.sendMessage(MiniMessage.miniMessage().deserialize(message));
    }

    private record PendingConnection(String previousServer, String currentServer)
    {
    }
}
