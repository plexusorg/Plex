package dev.plex.util;

import com.velocitypowered.api.proxy.ProxyServer;
import java.util.function.BooleanSupplier;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class PlexLog
{
    private static ProxyServer server;
    private static BooleanSupplier debugEnabled = () -> false;

    public static void configure(ProxyServer server, BooleanSupplier debugEnabled)
    {
        PlexLog.server = server;
        PlexLog.debugEnabled = debugEnabled == null ? () -> false : debugEnabled;
    }

    public static void log(String message, Object... strings)
    {
        for (int i = 0; i < strings.length; i++)
        {
            if (message.contains("{" + i + "}"))
            {
                message = message.replace("{" + i + "}", strings[i].toString());
            }
        }
        server.getConsoleCommandSource().sendMessage(MiniMessage.miniMessage().deserialize("<yellow>[Plex] <gray>" + message));
    }

    public static void log(Component component)
    {
        server.getConsoleCommandSource().sendMessage(Component.text("[Plex] ").color(NamedTextColor.YELLOW).append(component).colorIfAbsent(NamedTextColor.GRAY));
    }

    public static void error(String message, Object... strings)
    {
        for (int i = 0; i < strings.length; i++)
        {
            if (message.contains("{" + i + "}"))
            {
                message = message.replace("{" + i + "}", strings[i].toString());
            }
        }
        server.getConsoleCommandSource().sendMessage(MiniMessage.miniMessage().deserialize("<red>[Plex Error] <gold>" + message));
    }

    public static void warn(String message, Object... strings)
    {
        for (int i = 0; i < strings.length; i++)
        {
            if (message.contains("{" + i + "}"))
            {
                message = message.replace("{" + i + "}", strings[i].toString());
            }
        }
        server.getConsoleCommandSource().sendMessage(MiniMessage.miniMessage().deserialize("<#eb7c0e>[Plex Warning] <gold>" + message));
    }

    public static void debug(String message, Object... strings)
    {
        for (int i = 0; i < strings.length; i++)
        {
            if (message.contains("{" + i + "}"))
            {
                message = message.replace("{" + i + "}", strings[i].toString());
            }
        }
        if (debugEnabled.getAsBoolean())
        {
            server.getConsoleCommandSource().sendMessage(MiniMessage.miniMessage().deserialize("<dark_purple>[Plex Debug] <gold>" + message));
        }
    }
}
