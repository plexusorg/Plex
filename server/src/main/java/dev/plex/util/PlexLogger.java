package dev.plex.util;

import dev.plex.api.logging.LoggingApi;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.logger.slf4j.ComponentLogger;

public final class PlexLogger implements LoggingApi
{
    private final ComponentLogger logger = ComponentLogger.logger("");
    private boolean debugEnabled;

    @Override
    public void info(String message, Object... args)
    {
        logger.info(PlexUtils.mmDeserialize("<yellow>[Plex] <gray>" + format(message, args)));
    }

    public void info(Component component)
    {
        logger.info(Component.text("[Plex] ").color(NamedTextColor.YELLOW).append(component).colorIfAbsent(NamedTextColor.GRAY));
    }

    @Override
    public void error(String message, Object... args)
    {
        logger.error(PlexUtils.mmDeserialize("<red>[Plex Error] <gold>" + format(message, args)));
    }

    public void error(String message, Throwable throwable)
    {
        logger.error(PlexUtils.mmDeserialize("<red>[Plex Error] <gold>" + message), throwable);
    }

    @Override
    public void warn(String message, Object... args)
    {
        logger.warn(PlexUtils.mmDeserialize("<#eb7c0e>[Plex Warning] <gold>" + format(message, args)));
    }

    public void setDebugEnabled(boolean debugEnabled)
    {
        this.debugEnabled = debugEnabled;
    }

    @Override
    public void debug(String message, Object... args)
    {
        if (debugEnabled)
        {
            logger.info(PlexUtils.mmDeserialize("<dark_purple>[Plex Debug] <gold>" + format(message, args)));
        }
    }

    private static String format(String message, Object... args)
    {
        for (int i = 0; i < args.length; i++)
        {
            if (args[i] != null && message.contains("{" + i + "}"))
            {
                message = message.replace("{" + i + "}", args[i].toString());
            }
        }
        return message;
    }
}
