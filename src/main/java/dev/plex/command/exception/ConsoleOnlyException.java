package dev.plex.command.exception;

import static dev.plex.util.PlexUtils.messageString;

public class ConsoleOnlyException extends RuntimeException
{
    public ConsoleOnlyException()
    {
        super(messageString("consoleOnly"));
    }
}