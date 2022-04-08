package dev.plex.cmdblocker;

import dev.plex.rank.enums.Rank;
import lombok.Getter;

import java.util.regex.Pattern;

@Getter
public class RegexCommand extends BaseCommand
{
    public final Pattern regex;

    public RegexCommand(Pattern r1, Rank r2, String m1)
    {
        super(r2, m1);
        regex = r1;
    }
}