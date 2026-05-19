package dev.plex.command.exception;

/**
 * Signals that a command expected a banned player but none was found.
 */
public class PlayerNotBannedException extends RuntimeException
{
    /**
     * Creates an exception with the default player-not-banned marker message.
     */
    public PlayerNotBannedException()
    {
        super("PlayerNotBannedException");
    }

    /**
     * Creates an exception with a custom user-facing message.
     *
     * @param message failure message
     */
    public PlayerNotBannedException(String message)
    {
        super(message);
    }
}
