package dev.plex.command.exception;

public class CommandFailException extends RuntimeException // this is literally just a runtime exception lol
{
    public CommandFailException(String s)
    {
        super(s);
    }
}