package dev.plex.command.blocker;

import dev.plex.rank.enums.Rank;
import lombok.Getter;

@Getter
public class MatchCommand extends BaseCommand
{
    private final String match;

    public MatchCommand(String r1, Rank r2, String m1)
    {
        super(r2, m1);
        match = r1;
    }

    public String toString()
    {
        return "MatchCommand (Rank: " + (getRank() == null ? "ALL" : getRank().name()) + ", Match: " + match + ", Message: " + getMessage() + ")";
    }
}
