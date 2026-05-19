package dev.plex.command.exception;

public class PlayerNotBannedException extends RuntimeException
{
    public PlayerNotBannedException()
    {
        super("PlayerNotBannedException");
    }

    public PlayerNotBannedException(String message)
    {
        super(message);
    }
}
