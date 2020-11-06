package me.totalfreedom.plex.listener.impl;

import me.totalfreedom.plex.listener.PlexListener;
import me.totalfreedom.plex.util.PlexUtils;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.server.ServerListPingEvent;

public class ServerListener extends PlexListener
{
    @EventHandler
    public void onServerPing(ServerListPingEvent event)
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
            event.setMotd(motd.toString().trim());
        }
        else
        {
            event.setMotd(baseMotd.trim());
        }
    }
}
