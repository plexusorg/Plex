package dev.plex.util;

import dev.plex.Plex;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;

public class PlexLog
{
    private static final ComponentLogger logger = ComponentLogger.logger("");

    public static void log(String message, Object... strings)
    {
        for (int i = 0; i < strings.length; i++)
        {
            if (message.contains("{" + i + "}"))
            {
                message = message.replace("{" + i + "}", strings[i].toString());
            }
        }
        logger.info(PlexUtils.mmDeserialize("<yellow>[Plex] <gray>" + message));
    }

    public static void log(Component component)
    {
        logger.info(Component.text("[Plex] ").color(NamedTextColor.YELLOW).append(component).colorIfAbsent(NamedTextColor.GRAY));
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
        logger.error(PlexUtils.mmDeserialize("<red>[Plex Error] <gold>" + message));
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
        logger.warn(PlexUtils.mmDeserialize("<#eb7c0e>[Plex Warning] <gold>" + message));
    }

    public static void debug(String message, Object... strings)
    {
        if (Plex.get().config.getBoolean("debug"))
        {
            for (int i = 0; i < strings.length; i++)
            {
                if (message.contains("{" + i + "}"))
                {
                    message = message.replace("{" + i + "}", strings[i].toString());
                }
            }
            logger.info(PlexUtils.mmDeserialize("<dark_purple>[Plex Debug] <gold>" + message));
        }
    }
}
