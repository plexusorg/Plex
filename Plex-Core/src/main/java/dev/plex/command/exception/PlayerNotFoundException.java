package dev.plex.command.exception;

import static dev.plex.util.PlexUtils.messageString;

public class PlayerNotFoundException extends RuntimeException
{
    public PlayerNotFoundException()
    {
        super(messageString("playerNotFound"));
    }
}