package dev.plex.command.exception;

/**
 * Signals that a command can only be run from the console.
 */
public class ConsoleOnlyException extends RuntimeException
{
    /**
     * Creates an exception with the default console-only marker message.
     */
    public ConsoleOnlyException()
    {
        super("ConsoleOnlyException");
    }

    /**
     * Creates an exception with a custom user-facing message.
     *
     * @param message failure message
     */
    public ConsoleOnlyException(String message)
    {
        super(message);
    }
}
