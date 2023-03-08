package dev.plex.util;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class WebUtils
{
    public static Object simpleGET(String url)
    {
        try
        {
            URL u = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) u.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder content = new StringBuilder();
            while ((line = in.readLine()) != null)
            {
                content.append(line);
            }
            in.close();
            connection.disconnect();
            return new JSONParser().parse(content.toString());
        }
        catch (IOException | ParseException ex)
        {
            return null;
        }
    }

    public static UUID getFromName(String name)
    {
        JSONObject profile;
        profile = (JSONObject) simpleGET("https://api.ashcon.app/mojang/v2/user/" + name);
        if (profile == null)
        {
            PlexLog.error("Profile from Ashcon API returned null!");
            return null;
        }
        String uuidString = (String) profile.get("uuid");
        return UUID.fromString(uuidString);
    }
}
