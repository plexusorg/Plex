package dev.plex.command.exception;

public class CommandFailException extends RuntimeException
{
    public CommandFailException()
    {
        super("CommandFailException");
    }

    public CommandFailException(String message)
    {
        super(message);
    }
}
