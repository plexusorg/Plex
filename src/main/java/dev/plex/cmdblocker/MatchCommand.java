package dev.plex.cmdblocker;

import dev.plex.rank.enums.Rank;
import lombok.Getter;

@Getter
public class MatchCommand extends BaseCommand
{
    public final String match;

    public MatchCommand(String r1, Rank r2, String m1)
    {
        super(r2, m1);
        match = r1;
    }
}
