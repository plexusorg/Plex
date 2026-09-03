package dev.plex.util;

import net.kyori.adventure.text.Component;
import dev.plex.api.logging.LoggingApi;

public final class PlexLog
{
    private static final PlexLogger LOGGER = new PlexLogger();

    private PlexLog()
    {
    }

    public static LoggingApi api()
    {
        return LOGGER;
    }

    public static void log(String message, Object... strings)
    {
        LOGGER.info(message, strings);
    }

    public static void log(Component component)
    {
        LOGGER.info(component);
    }

    public static void error(String message, Object... strings)
    {
        LOGGER.error(message, strings);
    }

    public static void error(String message, Throwable throwable)
    {
        LOGGER.error(message, throwable);
    }

    public static void warn(String message, Object... strings)
    {
        LOGGER.warn(message, strings);
    }

    public static void setDebugEnabled(boolean debugEnabled)
    {
        LOGGER.setDebugEnabled(debugEnabled);
    }

    public static void debug(String message, Object... strings)
    {
        LOGGER.debug(message, strings);
    }
}
