package dev.plex.util;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import dev.plex.Plex;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class BungeeUtil
{
    public static void kickPlayer(Player player, Component message)
    {
        if (Bukkit.getServerConfig().isProxyEnabled())
        {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("KickPlayer");
            out.writeUTF(player.getName());
            out.writeUTF(LegacyComponentSerializer.legacySection().serialize(message));
            player.sendPluginMessage(Plex.get(), "BungeeCord", out.toByteArray());
        }
        else
        {
            player.kick(message);
        }
    }
}
