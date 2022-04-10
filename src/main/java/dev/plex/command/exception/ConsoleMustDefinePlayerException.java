package dev.plex.command.exception;

import static dev.plex.util.PlexUtils.messageString;

public class ConsoleMustDefinePlayerException extends RuntimeException
{
    public ConsoleMustDefinePlayerException()
    {
        super(messageString("consoleMustDefinePlayer"));
    }
}