package dev.plex.command.exception;

import static dev.plex.util.PlexUtils.tl;

public class ConsoleOnlyException extends RuntimeException
{
    public ConsoleOnlyException()
    {
        super(tl("consoleOnly"));
    }
}