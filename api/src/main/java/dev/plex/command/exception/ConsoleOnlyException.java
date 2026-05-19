package dev.plex.command.exception;

public class ConsoleOnlyException extends RuntimeException
{
    public ConsoleOnlyException()
    {
        super("ConsoleOnlyException");
    }

    public ConsoleOnlyException(String message)
    {
        super(message);
    }
}
