package dev.plex.util;

import dev.plex.Plex;
import lombok.Getter;

import java.io.InputStream;
import java.util.Properties;

public class BuildInfo
{
    @Getter
    public static String number;
    @Getter
    public static String author;
    @Getter
    public static String date;
    @Getter
    public static String head;

    public void load(Plex plugin)
    {
        try
        {
            Properties props;
            try (InputStream in = plugin.getResource("build.properties"))
            {
                props = new Properties();
                props.load(in);
            }

            number = props.getProperty("buildNumber", "unknown");
            author = props.getProperty("buildAuthor", "unknown");
            date = props.getProperty("buildDate", "unknown");
            head = props.getProperty("buildHead", "unknown");
        }
        catch (Exception ignored)
        {
        }
    }
}
