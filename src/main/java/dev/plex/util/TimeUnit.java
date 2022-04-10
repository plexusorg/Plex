package dev.plex.util;

public enum TimeUnit
{
    SECOND(1L),
    MINUTE(SECOND.get() * 60L),
    HOUR(MINUTE.get() * 60L),
    DAY(HOUR.get() * 24L),
    WEEK(DAY.get() * 7L),
    MONTH(DAY.get() * 30L),
    YEAR(MONTH.get() * 12L);

    private final long time;

    TimeUnit(long time)
    {
        this.time = time;
    }

    public long get()
    {
        return time;
    }
}