package dev.plex.util;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import dev.plex.PlexBase;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.json.JSONException;
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
import java.util.concurrent.atomic.AtomicReference;

public class UpdateChecker implements PlexBase
{
    /*
     * -4 = Never checked for updates
     * -3 = Likely rate limited
     * -2 = Unknown commit
     * -1 = Error occurred
     * 0 = Up to date
     * > 0 = Number of commits behind
     */
    private final String DOWNLOAD_PAGE = "https://ci.plex.us.org/job/";
    private final String REPO = plugin.config.getString("update_repo");
    private String BRANCH = plugin.config.getString("update_branch");
    private int distance = -4;

    // Adapted from Paper
    private int fetchDistanceFromGitHub(@Nonnull String repo, @Nonnull String branch, @Nonnull String hash)
    {
        try
        {
            HttpURLConnection connection = (HttpURLConnection) new URL("https://api.github.com/repos/" + repo + "/compare/" + branch + "..." + hash).openConnection();
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

    // If verbose is 0, it will display nothing
    // If verbose is 1, it will only display a message if there is an update available
    // If verbose is 2, it will display all messages
    public boolean getUpdateStatusMessage(CommandSender sender, boolean cached, int verbosity)
    {
        if (BRANCH == null)
        {
            PlexLog.error("You did not specify a branch to use for update checking. Defaulting to master.");
            BRANCH = "master";
        }
        // If it's -4, it hasn't checked for updates yet
        if (distance == -4)
        {
            distance = fetchDistanceFromGitHub(REPO, BRANCH, BuildInfo.getHead());
            PlexLog.debug("Never checked for updates, checking now...");
        }
        else
        {
            // If the request isn't asked to be cached, fetch it
            if (!cached)
            {
                distance = fetchDistanceFromGitHub(REPO, BRANCH, BuildInfo.getHead());
                PlexLog.debug("We have checked for updates before, but this request was not asked to be cached.");
            }
            else
            {
                PlexLog.debug("We have checked for updates before, using cache.");
            }
        }

        switch (distance)
        {
            case -1 ->
            {
                if (verbosity == 2)
                {
                    sender.sendMessage(Component.text("There was an error checking for updates.").color(NamedTextColor.RED));
                }
                return false;
            }
            case 0 ->
            {
                if (verbosity == 2)
                {
                    sender.sendMessage(Component.text("Plex is up to date!").color(NamedTextColor.GREEN));
                }
                return false;
            }
            case -2 ->
            {
                if (verbosity == 2)
                {
                    sender.sendMessage(Component.text("Unknown version, unable to check for updates.").color(NamedTextColor.RED));
                }
                return false;
            }
            default ->
            {
                if (verbosity >= 1)
                {
                    sender.sendMessage(Component.text("Plex is not up to date!", NamedTextColor.RED));
                    sender.sendMessage(Component.text("Download a new version at: " + DOWNLOAD_PAGE + "Plex").color(NamedTextColor.RED));
                    sender.sendMessage(Component.text("Or run: /plex update").color(NamedTextColor.RED));
                }
                return true;
            }
        }
    }

    public void updateJar(CommandSender sender, String name, boolean module)
    {
        CloseableHttpClient client = HttpClients.createDefault();
        AtomicReference<String> url = new AtomicReference<>(DOWNLOAD_PAGE + name);
        if (!module)
        {
            url.set(url.get() + "/job/" + BRANCH);
        }
        PlexLog.debug(url.toString());
        HttpGet get = new HttpGet(url + "/lastSuccessfulBuild/api/json");
        try
        {
            HttpResponse response = client.execute(get);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == HttpURLConnection.HTTP_OK)
            {
                JSONObject object = new JSONObject(EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8));
                JSONObject artifact = object.getJSONArray("artifacts").getJSONObject(0);
                String jarFile = artifact.getString("fileName");
                sender.sendMessage(PlexUtils.mmDeserialize("<green>Downloading latest JAR file: " + jarFile));
                File copyTo;
                if (!module)
                {
                    copyTo = new File(Bukkit.getUpdateFolderFile(), jarFile);
                }
                else
                {
                    copyTo = new File(plugin.getModulesFolder().getPath(), jarFile);
                }
                CompletableFuture.runAsync(() ->
                {
                    try
                    {
                        FileUtils.copyURLToFile(
                                new URL(url + "/lastSuccessfulBuild/artifact/build/libs/" + jarFile),
                                copyTo
                        );
                        sender.sendMessage(PlexUtils.mmDeserialize("<green>New JAR file downloaded successfully."));
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                });
            }
            else if (statusCode == HttpURLConnection.HTTP_NOT_FOUND)
            {
                sender.sendMessage(PlexUtils.mmDeserialize("<red>Could not update " + name + " as it can't be found on Jenkins."));
            }
            else
            {
                sender.sendMessage(PlexUtils.mmDeserialize("<red>Something went wrong while trying to update " + name + ". Please check the log for more information."));
                PlexLog.error("Unable to update module {0} due to unexpected status code returned from Jenkins - Status Code: {1}", name, statusCode);
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (JSONException e)
        {
            sender.sendMessage(PlexUtils.mmDeserialize("<red>Something went wrong while trying to gather information from Jenkins for " + name + ". Please check the log for more information"));
            PlexLog.error("Unable to parse JSON information received from Jenkins - see below for more information...");
            e.printStackTrace();
        }
    }
}
