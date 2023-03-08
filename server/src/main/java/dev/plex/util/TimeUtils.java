package dev.plex.util;

import dev.plex.Plex;
import org.apache.commons.lang3.math.NumberUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

public class TimeUtils
{
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy 'at' hh:mm:ss a z");
    private static final Set<String> TIMEZONES = Set.of(TimeZone.getAvailableIDs());
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
    public static String TIMEZONE = Plex.get().config.getString("server.timezone");

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
        ZonedDateTime time = ZonedDateTime.now(ZoneId.of(TimeUtils.TIMEZONE));
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

    public static String useTimezone(LocalDateTime date)
    {
        // Use UTC if the timezone is null or not set correctly
        if (TIMEZONE == null || !TIMEZONES.contains(TIMEZONE))
        {
            TIMEZONE = "Etc/UTC";
        }
        return DATE_FORMAT.withZone(ZoneId.of(TIMEZONE)).format(date);
    }

    public static String useTimezone(ZonedDateTime date)
    {
        // Use UTC if the timezone is null or not set correctly
        if (TIMEZONE == null || !TIMEZONES.contains(TIMEZONE))
        {
            TIMEZONE = "Etc/UTC";
        }
        return DATE_FORMAT.withZone(ZoneId.of(TIMEZONE)).format(date);
    }
}
