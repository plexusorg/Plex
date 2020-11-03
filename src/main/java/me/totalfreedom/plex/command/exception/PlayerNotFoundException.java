package me.totalfreedom.plex.command.exception;

import static me.totalfreedom.plex.util.PlexUtils.tl;

public class PlayerNotFoundException extends RuntimeException
{
    public PlayerNotFoundException()
    {
        super(tl("playerNotFound"));
    }
}