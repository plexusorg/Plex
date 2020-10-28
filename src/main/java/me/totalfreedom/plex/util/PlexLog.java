package me.totalfreedom.plex.util;

import me.totalfreedom.plex.Plex;

public class PlexLog
{
    public static void log(String message)
    {
        Plex.get().getServer().getConsoleSender().sendMessage(String.format("§e[Plex] §7%s", message));
    }

    public static void error(String message)
    {
        Plex.get().getServer().getConsoleSender().sendMessage(String.format("§c[Plex Error] §6%s", message));
    }
}
