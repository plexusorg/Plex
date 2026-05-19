package dev.plex.command.exception;

public class PlayerNotFoundException extends RuntimeException
{
    public PlayerNotFoundException()
    {
        super("PlayerNotFoundException");
    }

    public PlayerNotFoundException(String message)
    {
        super(message);
    }
}
