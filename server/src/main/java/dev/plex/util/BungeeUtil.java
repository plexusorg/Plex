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
    public static final boolean PROXIED_SERVER = isBungeeCord() || isVelocity();
    public static boolean isBungeeCord()
    {
        return Bukkit.spigot().getSpigotConfig().getBoolean("settings.bungeecord");
    }

    public static boolean isVelocity()
    {
        return Bukkit.spigot().getPaperConfig().getBoolean("settings.velocity-support.enabled") && !Bukkit.spigot().getPaperConfig().getString("settings.velocity-support.secret", "").isEmpty();
    }

    @SuppressWarnings("UnstableApiUsage")
    public static void kickPlayer(Player player, Component message)
    {
        if (PROXIED_SERVER)
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
