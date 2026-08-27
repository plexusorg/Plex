package dev.plex.network;

import dev.plex.Plex;
import dev.plex.meta.PlayerMeta;
import dev.plex.util.PlexLog;
import java.io.IOException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public final class ProxyVanishBridge implements PluginMessageListener
{
    private final Plex plugin;

    public ProxyVanishBridge(Plex plugin)
    {
        this.plugin = plugin;
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, VanishBridgeMessage.CHANNEL, this);
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, VanishBridgeMessage.CHANNEL);
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, byte @NotNull [] data)
    {
        if (!VanishBridgeMessage.CHANNEL.equals(channel))
        {
            return;
        }

        try
        {
            VanishBridgeMessage message = VanishBridgeMessage.decode(data);
            if (message.action() != VanishBridgeMessage.Action.QUERY || !message.playerId().equals(player.getUniqueId()))
            {
                return;
            }
            send(player, VanishBridgeMessage.state(player.getUniqueId(), PlayerMeta.isVanished(player)));
        }
        catch (IOException ex)
        {
            PlexLog.warn("Ignoring invalid proxy vanish message from {0}: {1}", player.getName(), ex.getMessage());
        }
    }

    public void hide(Player player, boolean silent)
    {
        send(player, VanishBridgeMessage.hide(player.getUniqueId(), silent));
    }

    public void show(Player player, boolean silent)
    {
        send(player, VanishBridgeMessage.show(player.getUniqueId(), silent));
    }

    private void send(Player player, VanishBridgeMessage message)
    {
        if (Bukkit.getServerConfig().isProxyEnabled())
        {
            player.sendPluginMessage(plugin, VanishBridgeMessage.CHANNEL, message.encode());
        }
    }
}
