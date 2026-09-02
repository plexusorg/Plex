package dev.plex.util;

import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.math.NumberUtils;

public class TimeUtils
{
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy 'at' hh:mm:ss a z");
    private static final List<String> timeUnits = new ArrayList<>()
    {{
        add("s");
        add("m");
        add("h");
        add("d");
        add("w");
        add("mo");
        add("y");
    }};
    private static final Map<String, ChronoUnit> CHRONO_UNITS = Map.of(
            "s", ChronoUnit.SECONDS,
            "m", ChronoUnit.MINUTES,
            "h", ChronoUnit.HOURS,
            "d", ChronoUnit.DAYS,
            "w", ChronoUnit.WEEKS,
            "mo", ChronoUnit.MONTHS,
            "y", ChronoUnit.YEARS);
    public static String TIMEZONE = "Etc/UTC";

    public static ZoneId zoneId()
    {
        try
        {
            return ZoneId.of(TIMEZONE);
        }
        catch (DateTimeException | NullPointerException e)
        {
            PlexLog.warn("\"{0}\" is not a valid timezone, using Etc/UTC instead", TIMEZONE);
            TIMEZONE = "Etc/UTC";
            return ZoneId.of(TIMEZONE);
        }
    }

    private static int parseInteger(String s) throws NumberFormatException
    {
        if (!NumberUtils.isCreatable(s))
        {
            throw new NumberFormatException();
        }
        return Integer.parseInt(s);
    }

    public static ZonedDateTime createDate(String arg)
    {
        ZonedDateTime time = ZonedDateTime.now(zoneId());
        for (String unit : timeUnits)
        {
            if (arg.endsWith(unit))
            {
                int duration = parseInteger(arg.substring(0, arg.length() - unit.length()));
                if (duration <= 0)
                {
                    throw new NumberFormatException();
                }
                try
                {
                    time = time.plus(duration, CHRONO_UNITS.get(unit));
                }
                catch (DateTimeException e)
                {
                    NumberFormatException invalid = new NumberFormatException();
                    invalid.initCause(e);
                    throw invalid;
                }
                return time;
            }
        }
        throw new NumberFormatException();
    }

    public static String useTimezone(LocalDateTime date)
    {
        return DATE_FORMAT.withZone(zoneId()).format(date);
    }

    public static String useTimezone(ZonedDateTime date)
    {
        return DATE_FORMAT.withZone(zoneId()).format(date);
    }

    public static String formatRelativeTime(ZonedDateTime date)
    {
        long seconds = ChronoUnit.SECONDS.between(ZonedDateTime.now(), date);

        if (seconds <= 0)
        {
            return "now";
        }

        RelativeUnit[] units = {
                new RelativeUnit("week", seconds / (7 * 24 * 60 * 60)),
                new RelativeUnit("day", seconds / (24 * 60 * 60)),
                new RelativeUnit("hour", seconds / (60 * 60)),
                new RelativeUnit("minute", seconds / 60),
                new RelativeUnit("second", seconds)
        };
        for (RelativeUnit unit : units)
        {
            if (unit.amount() > 0)
            {
                return unit.amount() + " " + unit.name() + (unit.amount() > 1 ? "s" : "");
            }
        }
        throw new IllegalStateException("positive duration has no display unit");
    }

    private record RelativeUnit(String name, long amount)
    {
    }

}
