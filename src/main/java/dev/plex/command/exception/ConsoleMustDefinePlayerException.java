package dev.plex.command.exception;

import static dev.plex.util.PlexUtils.tl;

public class ConsoleMustDefinePlayerException extends RuntimeException
{
    public ConsoleMustDefinePlayerException()
    {
        super(tl("consoleMustDefinePlayer"));
    }
}