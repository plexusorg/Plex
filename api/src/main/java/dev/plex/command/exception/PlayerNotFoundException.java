package dev.plex.command.exception;

/**
 * Signals that a command could not find the requested player.
 */
public class PlayerNotFoundException extends RuntimeException
{
    /**
     * Creates an exception with the default player-not-found marker message.
     */
    public PlayerNotFoundException()
    {
        super("PlayerNotFoundException");
    }

    /**
     * Creates an exception with a custom user-facing message.
     *
     * @param message failure message
     */
    public PlayerNotFoundException(String message)
    {
        super(message);
    }
}
