package dev.plex.command.exception;

/**
 * Signals that the console must supply a target player for a command.
 */
public class ConsoleMustDefinePlayerException extends RuntimeException
{
    /**
     * Creates an exception with the default console target marker message.
     */
    public ConsoleMustDefinePlayerException()
    {
        super("ConsoleMustDefinePlayerException");
    }

    /**
     * Creates an exception with a custom user-facing message.
     *
     * @param message failure message
     */
    public ConsoleMustDefinePlayerException(String message)
    {
        super(message);
    }
}
