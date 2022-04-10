package dev.plex.command.blocker;

import dev.plex.rank.enums.Rank;
import lombok.Getter;

@Getter
public class BaseCommand
{
    private final Rank rank;
    private final String message;

    public BaseCommand(Rank r, String m)
    {
        rank = r;
        message = m;
    }

    public String toString()
    {
        return "BaseCommand (Rank: " + (rank == null ? "ALL" : rank.name()) + ", Message: " + message + ")";
    }
}
