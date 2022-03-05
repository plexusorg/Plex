package dev.plex.util;

import dev.plex.PlexBase;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import org.bukkit.ChatColor;

public class UpdateChecker extends PlexBase
{
    private final String currentVersion = plugin.getDescription().getVersion();

    public boolean check()
    {
        if (currentVersion.contains("-SNAPSHOT"))
        {
            PlexLog.log("Snapshot version detected, not checking for updates.");
            return true;
        }
        try
        {
            String versionLink = "https://plex.us.org/updater/check/";
            URL url = new URL(versionLink);
            URLConnection con = url.openConnection();
            InputStreamReader isr = new InputStreamReader(con.getInputStream());
            BufferedReader reader = new BufferedReader(isr);
            if (!reader.ready())
            {
                return false;
            }
            String newVersion = reader.readLine();
            reader.close();

            if (!newVersion.equals(currentVersion))
            {
                PlexLog.log(ChatColor.RED + "There is a new version of Plex available: " + newVersion);
                return true;
            }
            else
            {
                return false;
            }
        }
        catch (IOException e)
        {
            PlexLog.error("There was an error checking for updates!");
            return false;
        }
    }
}
