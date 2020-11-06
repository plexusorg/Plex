package me.totalfreedom.plex.command.exception;

import static me.totalfreedom.plex.util.PlexUtils.tl;

public class ConsoleMustDefinePlayerException extends RuntimeException
{
    public ConsoleMustDefinePlayerException()
    {
        super(tl("consoleMustDefinePlayer"));
    }
}