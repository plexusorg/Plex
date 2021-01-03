package dev.plex.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class PlexLog
{
    public static void log(String message)
    {
        Bukkit.getConsoleSender().sendMessage(String.format(ChatColor.YELLOW + "[Plex] " + ChatColor.GRAY + "%s", message));
    }

    public static void error(String message)
    {
        Bukkit.getConsoleSender().sendMessage(String.format(ChatColor.RED + "[Plex Error]" + ChatColor.GOLD + "%s", message));
    }
}
