package dev.plex.command.exception;

import static dev.plex.util.PlexUtils.tl;

public class PlayerNotFoundException extends RuntimeException
{
    public PlayerNotFoundException()
    {
        super(tl("playerNotFound"));
    }
}