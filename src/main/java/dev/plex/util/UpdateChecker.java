package dev.plex.util;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import dev.plex.Plex;
import dev.plex.PlexBase;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class UpdateChecker extends PlexBase
{
    /*
     * -4 = Never checked for updates
     * -3 = Likely rate limited
     * -2 = Unknown commit
     * -1 = Error occurred
     * 0 = Up to date
     * > 0 = Number of commits behind
     */
    private final String DOWNLOAD_PAGE = "https://ci.plex.us.org/job/Plex/";
    private String branch = plugin.config.getString("update_branch");
    private int distance = -4;

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
            if (connection.getResponseCode() == HttpURLConnection.HTTP_FORBIDDEN)
            {
                return -3; // Rate limited likely
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

    public boolean getUpdateStatusMessage(CommandSender sender, boolean cached, boolean verbose)
    {
        if (branch == null)
        {
            PlexLog.error("You did not specify a branch to use for update checking. Defaulting to master.");
            branch = "master";
        }
        // If it's -4, it hasn't checked for updates yet
        if (distance == -4)
        {
            distance = fetchDistanceFromGitHub("plexusorg/Plex", branch, Plex.build.head);
            PlexLog.debug("Never checked for updates, checking now...");
        }
        else
        {
            // If the request isn't asked to be cached, fetch it
            if (!cached)
            {
                distance = fetchDistanceFromGitHub("plexusorg/Plex", branch, Plex.build.head);
                PlexLog.debug("We have checked for updates before, but this request was not asked to be cached.");
            }
            else
            {
                PlexLog.debug("We have checked for updates before, using cache.");
            }
        }

        switch (distance)
        {
            case -1 -> {
                if (verbose)
                {
                    sender.sendMessage(Component.text("There was an error checking for updates.").color(NamedTextColor.RED));
                }
                return false;
            }
            case 0 -> {
                if (verbose)
                {
                    sender.sendMessage(Component.text("Your version of Plex is up to date!").color(NamedTextColor.GREEN));
                }
                return false;
            }
            case -2 -> {
                if (verbose)
                {
                    sender.sendMessage(Component.text("Unknown version, unable to check for updates.").color(NamedTextColor.RED));
                }
                return false;
            }
            default -> {
                sender.sendMessage(Component.text("Your version of Plex is not up to date!", NamedTextColor.RED));
                sender.sendMessage(Component.text("Download a new version at: " + DOWNLOAD_PAGE).color(NamedTextColor.RED));
                sender.sendMessage(Component.text("Or run: /plex update").color(NamedTextColor.RED));
                return true;
            }
        }
    }

    private void sendMini(CommandSender sender, String message)
    {
        sender.sendMessage(MiniMessage.miniMessage().deserialize(message));
    }

    public void updateJar(CommandSender sender)
    {
        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet(DOWNLOAD_PAGE + "job/" + branch + "/lastSuccessfulBuild/api/json");
        try
        {
            HttpResponse response = client.execute(get);
            JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));
            JSONObject artifact = object.getJSONArray("artifacts").getJSONObject(0);
            String name = artifact.getString("fileName");
            sendMini(sender, "Downloading latest Plex jar file: " + name);
            CompletableFuture.runAsync(() ->
            {
                try
                {
                    FileUtils.copyURLToFile(
                            new URL(DOWNLOAD_PAGE + "job/" + branch + "/lastSuccessfulBuild/artifact/build/libs/" + name),
                            new File(Bukkit.getUpdateFolderFile(), name)
                    );
                    sendMini(sender, "Saved new jar. Please restart your server.");
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            });
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
