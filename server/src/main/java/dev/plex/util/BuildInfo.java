package dev.plex.util;

import dev.plex.Plex;

import java.io.InputStream;
import java.util.Properties;

import lombok.Getter;

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
    @Getter
    public static String minecraftVersion;

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
            minecraftVersion = props.getProperty("minecraftVersion", "unknown");
        }
        catch (Exception ignored)
        {
        }
    }

    public static String shortenCommit(String commit)
    {
        if (commit == null || commit.length() <= 7)
        {
            return commit;
        }
        return commit.substring(0, 7);
    }
}
