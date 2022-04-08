package dev.plex.cmdblocker;

import dev.plex.rank.enums.Rank;
import lombok.Getter;

@Getter
public class BaseCommand
{
    public final Rank rank;
    public final String message;

    public BaseCommand(Rank r, String m)
    {
        rank = r;
        message = m;
    }
}
