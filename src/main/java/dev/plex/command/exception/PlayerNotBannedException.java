package dev.plex.command.exception;

import static dev.plex.util.PlexUtils.tl;

public class PlayerNotBannedException extends RuntimeException
{
    public PlayerNotBannedException()
    {
        super(tl("playerNotBanned"));
    }
}