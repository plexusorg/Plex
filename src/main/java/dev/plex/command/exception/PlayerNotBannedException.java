package dev.plex.command.exception;

import static dev.plex.util.PlexUtils.messageString;

public class PlayerNotBannedException extends RuntimeException
{
    public PlayerNotBannedException()
    {
        super(messageString("playerNotBanned"));
    }
}