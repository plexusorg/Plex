package dev.plex.listener.impl;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import dev.plex.listener.PlexListener;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;

import java.util.List;
import java.util.stream.Collectors;

public class ServerListener extends PlexListener
{
    @EventHandler
    public void onServerPing(PaperServerListPingEvent event)
    {
        String baseMotd = plugin.config.getString("server.motd");
        baseMotd = baseMotd.replace("\\n", "\n");
        baseMotd = baseMotd.replace("%servername%", plugin.config.getString("server.name"));
        baseMotd = baseMotd.replace("%mcversion%", Bukkit.getBukkitVersion().split("-")[0]);
        if (plugin.config.getBoolean("server.colorize_motd"))
        {
            final StringBuilder motd = new StringBuilder();
            for (final String word : baseMotd.split(" "))
            {
                motd.append(PlexUtils.randomChatColor()).append(word).append(" ");
            }
            event.motd(LegacyComponentSerializer.legacyAmpersand().deserialize(motd.toString().trim()));
        } else
        {
            event.motd(LegacyComponentSerializer.legacyAmpersand().deserialize(baseMotd.trim()));
        }
        if (plugin.config.contains("server.sample"))
        {
            List<String> samples = plugin.config.getStringList("server.sample");
            if (!samples.isEmpty())
            {
                event.getPlayerSample().clear();
                event.getPlayerSample().addAll(samples.stream().map(string -> string.replace("&", "§")).map(Bukkit::createProfile).collect(Collectors.toList()));
            }
        }
    }
}
