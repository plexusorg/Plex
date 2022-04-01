package dev.plex.util;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import dev.plex.Plex;
import dev.plex.PlexBase;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.annotation.Nonnull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class UpdateChecker extends PlexBase
{
    private static final String DOWNLOAD_PAGE = "https://ci.plex.us.org/job/Plex/";

    // Adapted from Paper
    private int fetchDistanceFromGitHub(@Nonnull String repo, @Nonnull String branch, @Nonnull String hash)
    {
        try
        {
            HttpURLConnection connection = (HttpURLConnection)new URL("https://api.github.com/repos/" + repo + "/compare/" + branch + "..." + hash).openConnection();
            connection.connect();
            if (connection.getResponseCode() == HttpURLConnection.HTTP_NOT_FOUND)
            {
                return -2; // Unknown commit
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), Charsets.UTF_8)))
            {
                JsonObject obj = new Gson().fromJson(reader, JsonObject.class);
                String status = obj.get("status").getAsString();
                return switch (status)
                        {
                            case "identical" -> 0;
                            case "behind" -> obj.get("behind_by").getAsInt();
                            default -> -1;
                        };
            }
            catch (JsonSyntaxException | NumberFormatException e)
            {
                e.printStackTrace();
                return -1;
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return -1;
        }
    }

    public boolean getUpdateStatusMessage(CommandSender sender)
    {
        int distance;
        distance = fetchDistanceFromGitHub("plexusorg/Plex", "master", Plex.build.head);

        switch (distance)
        {
            case -1 -> {
                sender.sendMessage(Component.text("There was an error checking for updates.").color(NamedTextColor.RED));
                return false;
            }
            case 0 -> {
                sender.sendMessage(Component.text("Your version of Plex is up to date!").color(NamedTextColor.GREEN));
                return true;
            }
            case -2 -> {
                sender.sendMessage(Component.text("Unknown version, unable to check for updates.").color(NamedTextColor.RED));
                return false;
            }
            default -> {
                sender.sendMessage(Component.text("Your version of Plex is not up to date!", NamedTextColor.RED));
                sender.sendMessage(Component.text("Download a new version at: " + DOWNLOAD_PAGE).color(NamedTextColor.RED));
                return true;
            }
        }
    }
}
