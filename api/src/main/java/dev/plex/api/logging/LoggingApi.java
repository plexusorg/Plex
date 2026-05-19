package dev.plex.api.logging;

public interface LoggingApi
{
    void info(String message, Object... args);
    void debug(String message, Object... args);
    void warn(String message, Object... args);
    void error(String message, Object... args);
}
