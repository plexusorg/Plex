package dev.plex.command.exception;

/**
 * Signals that command execution failed with a user-facing message.
 */
public class CommandFailException extends RuntimeException
{
    /**
     * Creates an exception with the default command failure marker message.
     */
    public CommandFailException()
    {
        super("CommandFailException");
    }

    /**
     * Creates an exception with a custom user-facing failure message.
     *
     * @param message failure message
     */
    public CommandFailException(String message)
    {
        super(message);
    }
}
