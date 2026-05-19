package dev.plex.api.logging;

/**
 * Logging facade exposed through the Plex API.
 */
public interface LoggingApi
{
    /**
     * Writes an informational log message.
     *
     * @param message message template
     * @param args template arguments
     */
    void info(String message, Object... args);

    /**
     * Writes a debug log message.
     *
     * @param message message template
     * @param args template arguments
     */
    void debug(String message, Object... args);

    /**
     * Writes a warning log message.
     *
     * @param message message template
     * @param args template arguments
     */
    void warn(String message, Object... args);

    /**
     * Writes an error log message.
     *
     * @param message message template
     * @param args template arguments
     */
    void error(String message, Object... args);
}
