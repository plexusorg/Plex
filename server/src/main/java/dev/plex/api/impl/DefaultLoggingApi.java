package dev.plex.api.impl;

import dev.plex.api.logging.LoggingApi;
import dev.plex.util.PlexLog;

final class DefaultLoggingApi implements LoggingApi
{
    @Override public void info(String message, Object... args) { PlexLog.log(message, args); }
    @Override public void debug(String message, Object... args) { PlexLog.debug(message, args); }
    @Override public void warn(String message, Object... args) { PlexLog.warn(message, args); }
    @Override public void error(String message, Object... args) { PlexLog.error(message, args); }
}
