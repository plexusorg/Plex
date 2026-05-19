package dev.plex.util;

import dev.plex.Plex;
import dev.plex.updater.ArtifactMetadata;
import dev.plex.updater.UpdateChannel;
import dev.plex.updater.UpdateMetadataClient;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UpdateChecker
{
    private static final long PLEX_METADATA_FAILURE_CACHE_MILLIS = 5 * 60 * 1000L;

    private final Plex plugin;
    private final UpdateChannel channel;
    private final UpdateMetadataClient metadataClient;
    private ArtifactMetadata latestPlexMetadata;
    private UpdateMetadataClient.MetadataException latestPlexMetadataFailure;
    private long latestPlexMetadataFailureAtMillis;

    public UpdateChecker(Plex plugin)
    {
        this.plugin = plugin;
        this.channel = UpdateChannel.fromConfig(plugin.config.getString("updater.channel"));
        this.metadataClient = new UpdateMetadataClient(channel);
    }

    // If verbose is 0, it will display nothing
    // If verbose is 1, it will only display a message if there is an update available
    // If verbose is 2, it will display all messages
    public boolean getUpdateStatusMessage(CommandSender sender, boolean cached, int verbosity)
    {
        try
        {
            ArtifactMetadata metadata = fetchLatestPlexMetadata(cached);
            if (metadata.matchesCurrentBuild(plugin.getPluginMeta().getVersion(), BuildInfo.getNumber(), BuildInfo.getCommit()))
            {
                if (verbosity == 2)
                {
                    sendMessage(sender, PlexUtils.messageComponent("updateUpToDate", channel.id()));
                }
                return false;
            }

            if (verbosity >= 1)
            {
                sendMessage(sender, PlexUtils.messageComponent("updateAvailable", metadata.version(), channel.id()));
                sendMessage(sender, PlexUtils.messageComponent("updateRunCommand"));
            }
            return true;
        }
        catch (UpdateMetadataClient.MetadataException e)
        {
            if (verbosity == 2 || (verbosity >= 1 && !e.notFound()))
            {
                sendMessage(sender, updateMetadataErrorComponent(e));
            }
            if (!e.notFound())
            {
                PlexLog.error("Unable to check for updates: {0}", e.getMessage());
                if (e.getCause() != null)
                {
                    e.getCause().printStackTrace();
                }
            }
            return false;
        }
    }

    public void updateJar(CommandSender sender, String name, boolean module)
    {
        try
        {
            ArtifactMetadata metadata = module
                    ? metadataClient.fetchModuleLatest(name, plugin.getApi().compatibility().version())
                    : fetchLatestPlexMetadata(false);

            if (!module && metadata.matchesCurrentBuild(plugin.getPluginMeta().getVersion(), BuildInfo.getNumber(), BuildInfo.getCommit()))
            {
                sendMessage(sender, PlexUtils.messageComponent("updateAlreadyUpToDate", channel.id()));
                return;
            }

            File copyTo = module
                    ? new File(plugin.getModulesFolder(), metadata.fileName())
                    : new File(Bukkit.getUpdateFolderFile(), metadata.fileName());

            sendMessage(sender, PlexUtils.messageComponent("updateDownloading", metadata.fileName()));
            plugin.getApi().scheduler().runAsync(() -> downloadAndInstall(sender, metadata, copyTo));
        }
        catch (UpdateMetadataClient.MetadataException e)
        {
            sendMessage(sender, updateMetadataErrorComponent(e));
            if (!e.notFound())
            {
                PlexLog.error("Unable to update {0}: {1}", name, e.getMessage());
                if (e.getCause() != null)
                {
                    e.getCause().printStackTrace();
                }
            }
        }
    }

    private synchronized ArtifactMetadata fetchLatestPlexMetadata(boolean cached) throws UpdateMetadataClient.MetadataException
    {
        if (cached)
        {
            if (latestPlexMetadata != null)
            {
                return latestPlexMetadata;
            }
            if (latestPlexMetadataFailure != null && isPlexMetadataFailureCacheFresh())
            {
                throw latestPlexMetadataFailure;
            }
        }

        try
        {
            latestPlexMetadata = metadataClient.fetchPlexLatest(BuildInfo.getMinecraftVersion());
            latestPlexMetadataFailure = null;
            latestPlexMetadataFailureAtMillis = 0L;
            return latestPlexMetadata;
        }
        catch (UpdateMetadataClient.MetadataException e)
        {
            if (latestPlexMetadata == null)
            {
                latestPlexMetadataFailure = e;
                latestPlexMetadataFailureAtMillis = System.currentTimeMillis();
            }
            throw e;
        }
    }

    private boolean isPlexMetadataFailureCacheFresh()
    {
        return System.currentTimeMillis() - latestPlexMetadataFailureAtMillis < PLEX_METADATA_FAILURE_CACHE_MILLIS;
    }

    private void downloadAndInstall(CommandSender sender, ArtifactMetadata metadata, File copyTo)
    {
        File parent = copyTo.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs())
        {
            sendMessage(sender, PlexUtils.messageComponent("updateDirectoryFailed", parent.getAbsolutePath()));
            return;
        }

        File temporaryFile = new File(parent, copyTo.getName() + ".download");
        try
        {
            download(metadata.downloadUrl(), temporaryFile);
            validateDownloadedFile(metadata, temporaryFile);
            Files.move(temporaryFile.toPath(), copyTo.toPath(), StandardCopyOption.REPLACE_EXISTING);
            sendMessage(sender, PlexUtils.messageComponent("updateDownloaded"));
        }
        catch (IOException e)
        {
            sendMessage(sender, PlexUtils.messageComponent("updateDownloadFailed", metadata.name()));
            PlexLog.error("Unable to download update {0}: {1}", metadata.name(), e.getMessage());
            e.printStackTrace();
        }
        finally
        {
            try
            {
                Files.deleteIfExists(temporaryFile.toPath());
            }
            catch (IOException ignored)
            {
            }
        }
    }

    private void download(String downloadUrl, File destination) throws IOException
    {
        HttpURLConnection connection = (HttpURLConnection) URI.create(downloadUrl).toURL().openConnection();
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(30000);

        int statusCode = connection.getResponseCode();
        if (statusCode != HttpURLConnection.HTTP_OK)
        {
            throw new IOException("download request returned HTTP " + statusCode);
        }

        try (InputStream inputStream = connection.getInputStream())
        {
            Files.copy(inputStream, destination.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void validateDownloadedFile(ArtifactMetadata metadata, File file) throws IOException
    {
        if (metadata.size() != null && metadata.size() >= 0 && Files.size(file.toPath()) != metadata.size())
        {
            throw new IOException("downloaded file size did not match metadata size");
        }

        String actualSha256 = sha256(file);
        if (!metadata.sha256().equalsIgnoreCase(actualSha256))
        {
            throw new IOException("downloaded file SHA-256 did not match metadata SHA-256");
        }
    }

    private String sha256(File file) throws IOException
    {
        MessageDigest digest;
        try
        {
            digest = MessageDigest.getInstance("SHA-256");
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IllegalStateException("SHA-256 is not available", e);
        }

        try (InputStream inputStream = Files.newInputStream(file.toPath());
             DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest))
        {
            digestInputStream.transferTo(OutputStream.nullOutputStream());
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private Component updateMetadataErrorComponent(UpdateMetadataClient.MetadataException e)
    {
        if (e.notFound())
        {
            return PlexUtils.messageComponent("updateMetadataNotFound", channel.id());
        }
        return PlexUtils.messageComponent("updateMetadataError", e.getMessage());
    }

    private void sendMessage(CommandSender sender, Component message)
    {
        if (sender instanceof Player player)
        {
            plugin.getApi().scheduler().runEntity(player, () -> sender.sendMessage(message));
            return;
        }
        plugin.getApi().scheduler().runGlobal(() -> sender.sendMessage(message));
    }
}
