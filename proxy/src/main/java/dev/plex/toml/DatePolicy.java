package dev.plex.toml;

import java.util.TimeZone;

public class DatePolicy
{

    private final TimeZone timeZone;
    private final boolean showFractionalSeconds;

    DatePolicy(TimeZone timeZone, boolean showFractionalSeconds)
    {
        this.timeZone = timeZone;
        this.showFractionalSeconds = showFractionalSeconds;
    }

    TimeZone getTimeZone()
    {
        return timeZone;
    }

    boolean isShowFractionalSeconds()
    {
        return showFractionalSeconds;
    }
}
