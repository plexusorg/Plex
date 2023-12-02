package dev.plex.util;

import dev.plex.Plex;
import lombok.Getter;

import java.io.InputStream;
import java.util.Properties;

public class BuildInfo
{
    @Getter
    public static String author;
    @Getter
    public static String commit;
    @Getter
    public static String date;
    @Getter
    public static String number;

    public void load(Plex plugin)
    {
        try
        {
            Properties props;
            try (InputStream in = plugin.getResource("build-vars.properties"))
            {
                props = new Properties();
                props.load(in);
            }

            author = props.getProperty("author", "unknown");
            commit = props.getProperty("gitCommit", "unknown");
            date = props.getProperty("date", "unknown");
            number = props.getProperty("buildNumber", "unknown");
        }
        catch (Exception ignored)
        {
        }
    }
}
