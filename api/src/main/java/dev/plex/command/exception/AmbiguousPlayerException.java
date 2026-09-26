package dev.plex.command.exception;

import java.util.List;

public class AmbiguousPlayerException extends RuntimeException
{
    private final List<String> matchingNames;

    public AmbiguousPlayerException(List<String> matchingNames)
    {
        super("Ambiguous player: " + String.join(", ", matchingNames));
        this.matchingNames = List.copyOf(matchingNames);
    }

    public List<String> getMatchingNames()
    {
        return matchingNames;
    }
}
