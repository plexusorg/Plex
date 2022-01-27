package dev.plex.util;

import dev.plex.Plex;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

public class PlexLog
{
    private static final boolean debugEnabled = Plex.get().config.getBoolean("debug");

    public static void log(String message)
    {
        Bukkit.getConsoleSender().sendMessage(String.format(ChatColor.YELLOW + "[Plex] " + ChatColor.GRAY + "%s", message));
    }

    public static void error(String message)
    {
        Bukkit.getConsoleSender().sendMessage(String.format(ChatColor.RED + "[Plex Error] " + ChatColor.GOLD + "%s", message));
    }

    public static void debug(String message)
    {
        if (debugEnabled)
        {
            Bukkit.getConsoleSender().sendMessage(String.format(ChatColor.DARK_PURPLE + "[Plex Debug] " + ChatColor.GOLD + "%s", message));
        }
    }
}
