package dev.plex.command.exception;

public class ConsoleMustDefinePlayerException extends RuntimeException
{
    public ConsoleMustDefinePlayerException()
    {
        super("ConsoleMustDefinePlayerException");
    }

    public ConsoleMustDefinePlayerException(String message)
    {
        super(message);
    }
}
