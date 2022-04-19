package dev.plex.util;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import org.apache.commons.lang.math.NumberUtils;

public class TimeUtils
{
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

    private static int parseInteger(String s) throws NumberFormatException
    {
        if (!NumberUtils.isNumber(s))
        {
            throw new NumberFormatException();
        }
        return Integer.parseInt(s);
    }

    public static LocalDateTime createDate(String arg)
    {
        LocalDateTime time = LocalDateTime.now();
        for (String unit : timeUnits)
        {
            if (arg.endsWith(unit))
            {
                int duration = parseInteger(arg.replace(unit, ""));
                switch (unit)
                {
                    case "y" -> time = time.plusYears(duration);
                    case "mo" -> time = time.plusMonths(duration);
                    case "w" -> time = time.plusWeeks(duration);
                    case "d" -> time = time.plusDays(duration);
                    case "h" -> time = time.plusHours(duration);
                    case "m" -> time = time.plusMinutes(duration);
                    case "s" -> time = time.plusSeconds(duration);
                }
            }
        }
        return time;
    }

    public static long getDateNow()
    {
        return new Date().getTime();
    }

    public static Date getDateFromLong(long epoch)
    {
        return new Date(epoch);
    }

    public static long hoursToSeconds(long hours)
    {
        return hours * 3600;
    }

    public static long minutesToSeconds(long minutes)
    {
        return minutes * 60;
    }
}
