package dev.plex.util;

import dev.plex.Plex;
import dev.plex.module.PlexModule;
import dev.plex.module.PlexModuleFile;
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
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

public class UpdateChecker
{
    private static final long PLEX_METADATA_FAILURE_CACHE_MILLIS = 5 * 60 * 1000L;
    private static final long MAX_ARTIFACT_BYTES = 256L * 1024L * 1024L;

    private final Plex plugin;
    private final UpdateChannel channel;
    private final UpdateMetadataClient metadataClient;
    private ArtifactMetadata latestPlexMetadata;
    private UpdateMetadataClient.MetadataException latestPlexMetadataFailure;
    private long latestPlexMetadataFailureAtMillis;
    private ArtifactMetadata latestPlexChannelLatestMetadata;
    private UpdateMetadataClient.MetadataException latestPlexChannelLatestMetadataFailure;
    private long latestPlexChannelLatestMetadataFailureAtMillis;

    public enum UpdateCheckStatus
    {
        UP_TO_DATE, UPDATE_AVAILABLE, MINECRAFT_TOO_OLD, MINECRAFT_TOO_NEW, MINECRAFT_UNLISTED, ERROR
    }

    public record UpdateCheckResult(UpdateCheckStatus status, ArtifactMetadata metadata, UpdateMetadataClient.MetadataException error)
    {
    }

    public enum ModuleUpdateResult
    {
        UPDATED, SKIPPED, FAILED
    }

    public UpdateChecker(Plex plugin)
    {
        this.plugin = plugin;
        this.channel = UpdateChannel.fromConfig(plugin.config.getString("updater.channel"));
        this.metadataClient = new UpdateMetadataClient(channel);
    }

    public synchronized void clearCache()
    {
        latestPlexMetadata = null;
        latestPlexMetadataFailure = null;
        latestPlexMetadataFailureAtMillis = 0L;
        latestPlexChannelLatestMetadata = null;
        latestPlexChannelLatestMetadataFailure = null;
        latestPlexChannelLatestMetadataFailureAtMillis = 0L;
    }

    public UpdateCheckResult checkForUpdates(boolean useCache)
    {
        String runningMinecraftVersion = getRunningMinecraftVersion();
        if (runningMinecraftVersion == null || runningMinecraftVersion.isBlank() || "unknown".equalsIgnoreCase(runningMinecraftVersion))
        {
            return new UpdateCheckResult(UpdateCheckStatus.ERROR, null, UpdateMetadataClient.MetadataException.localError("running Minecraft version could not be determined"));
        }

        try
        {
            ArtifactMetadata metadata = fetchLatestPlexMetadata(useCache, runningMinecraftVersion);
            if (metadata.matchesCurrentBuild(plugin.getPluginMeta().getVersion(), BuildInfo.getNumber(), BuildInfo.getCommit()))
            {
                return new UpdateCheckResult(UpdateCheckStatus.UP_TO_DATE, metadata, null);
            }
            return new UpdateCheckResult(UpdateCheckStatus.UPDATE_AVAILABLE, metadata, null);
        }
        catch (UpdateMetadataClient.MetadataException e)
        {
            if (!e.notFound())
            {
                return new UpdateCheckResult(UpdateCheckStatus.ERROR, null, e);
            }
        }

        ArtifactMetadata channelLatest;
        try
        {
            channelLatest = fetchPlexChannelLatestMetadata(useCache);
        }
        catch (UpdateMetadataClient.MetadataException e)
        {
            return new UpdateCheckResult(UpdateCheckStatus.ERROR, null, e);
        }

        if (channelLatest.matchesCurrentBuild(plugin.getPluginMeta().getVersion(), BuildInfo.getNumber(), BuildInfo.getCommit()))
        {
            return new UpdateCheckResult(UpdateCheckStatus.UP_TO_DATE, channelLatest, null);
        }

        List<String> supportedVersions = sortedSupportedVersions(channelLatest);
        if (!supportedVersions.isEmpty())
        {
            String minimumVersion = supportedVersions.get(0);
            String maximumVersion = supportedVersions.get(supportedVersions.size() - 1);
            if (compareMinecraftVersions(runningMinecraftVersion, minimumVersion) < 0)
            {
                return new UpdateCheckResult(UpdateCheckStatus.MINECRAFT_TOO_OLD, channelLatest, null);
            }
            if (compareMinecraftVersions(runningMinecraftVersion, maximumVersion) > 0)
            {
                return new UpdateCheckResult(UpdateCheckStatus.MINECRAFT_TOO_NEW, channelLatest, null);
            }
        }
        return new UpdateCheckResult(UpdateCheckStatus.MINECRAFT_UNLISTED, channelLatest, null);
    }

    public void sendResultMessage(CommandSender sender, UpdateCheckResult result, int verbosity)
    {
        switch (result.status())
        {
            case UP_TO_DATE:
                if (verbosity == 2)
                {
                    sendMessage(sender, PlexUtils.messageComponent("updateUpToDate", channel.id()));
                }
                break;
            case UPDATE_AVAILABLE:
                if (verbosity >= 1)
                {
                    sendMessage(sender, PlexUtils.messageComponent("updateAvailable", result.metadata().version(), channel.id()));
                    sendMessage(sender, PlexUtils.messageComponent("updateRunCommand"));
                }
                break;
            case MINECRAFT_TOO_OLD:
                if (verbosity >= 1)
                {
                    List<String> supportedVersions = sortedSupportedVersions(result.metadata());
                    sendMessage(sender, PlexUtils.messageComponent("updateRequiresNewerMinecraft",
                            result.metadata().version(), channel.id(), firstSupportedVersion(supportedVersions), getRunningMinecraftVersion(), String.join(", ", supportedVersions)));
                }
                break;
            case MINECRAFT_TOO_NEW:
                if (verbosity >= 1)
                {
                    List<String> supportedVersions = sortedSupportedVersions(result.metadata());
                    sendMessage(sender, PlexUtils.messageComponent("updateUnsupportedNewerMinecraft",
                            result.metadata().version(), channel.id(), lastSupportedVersion(supportedVersions), getRunningMinecraftVersion(), String.join(", ", supportedVersions)));
                }
                break;
            case MINECRAFT_UNLISTED:
                if (verbosity >= 1)
                {
                    List<String> supportedVersions = sortedSupportedVersions(result.metadata());
                    sendMessage(sender, PlexUtils.messageComponent("updateRequiresMinecraftVersion",
                            result.metadata().version(), channel.id(), String.join(", ", supportedVersions), getRunningMinecraftVersion()));
                }
                break;
            case ERROR:
                UpdateMetadataClient.MetadataException error = result.error();
                if (error.notFound())
                {
                    if (verbosity == 2)
                    {
                        sendMessage(sender, PlexUtils.messageComponent("updateMetadataNotFound", channel.id()));
                    }
                    break;
                }
                if (verbosity >= 1)
                {
                    if (sender instanceof ConsoleCommandSender)
                    {
                        PlexLog.warn("Unable to check for updates right now; the updater will try again later.");
                    }
                    else
                    {
                        sendMessage(sender, updateMetadataErrorComponent(error));
                    }
                    PlexLog.debug("Update metadata check failed: {0}", error.getMessage());
                }
                break;
        }
    }

    // If verbose is 0, it will display nothing
    // If verbose is 1, it will only display a message if there is an update available
    // If verbose is 2, it will display all messages
    public boolean getUpdateStatusMessage(CommandSender sender, boolean cached, int verbosity)
    {
        UpdateCheckResult result = checkForUpdates(cached);
        sendResultMessage(sender, result, verbosity);
        return result.status() == UpdateCheckStatus.UPDATE_AVAILABLE;
    }

    public void updateJar(CommandSender sender, ArtifactMetadata metadata, Runnable onSuccess)
    {
        File copyTo = new File(Bukkit.getUpdateFolderFile(), metadata.fileName());
        sendMessage(sender, PlexUtils.messageComponent("updateDownloading", metadata.fileName()));
        plugin.getApi().scheduler().runAsync(() -> downloadAndInstall(sender, metadata, copyTo, onSuccess));
    }

    public void installModuleJar(CommandSender sender, String name)
    {
        updateJar(sender, name, List.of(), () ->
        {
            plugin.getModuleManager().reloadModules();
            sendMessage(sender, PlexUtils.messageComponent("moduleRestartRequired"));
        });
    }

    public ModuleUpdateResult updateModuleJar(CommandSender sender, PlexModule module)
    {
        PlexModuleFile moduleFile = module.getPlexModuleFile();
        if (!moduleFile.isUpdaterEnabled())
        {
            sendMessage(sender, PlexUtils.messageComponent("moduleUpdateDisabled", moduleFile.getName()));
            return ModuleUpdateResult.SKIPPED;
        }
        try
        {
            ArtifactMetadata metadata = metadataClient.fetchModuleLatest(moduleFile.getName(), plugin.getApi().apiCompatibilityVersion(), moduleFile.getUpdateUrls());
            File copyTo = new File(plugin.getModulesFolder(), metadata.fileName());

            sendMessage(sender, PlexUtils.messageComponent("updateDownloading", metadata.fileName()));
            return downloadAndInstall(sender, metadata, copyTo, null)
                    ? ModuleUpdateResult.UPDATED
                    : ModuleUpdateResult.FAILED;
        }
        catch (UpdateMetadataClient.MetadataException e)
        {
            sendMessage(sender, updateMetadataErrorComponent(e));
            if (!e.notFound())
            {
                PlexLog.error("Unable to update {0}: {1}", moduleFile.getName(), e.getMessage());
            }
            return ModuleUpdateResult.FAILED;
        }
    }

    private void updateJar(CommandSender sender, String name, List<String> moduleUpdateUrls, Runnable onSuccess)
    {
        try
        {
            ArtifactMetadata metadata = metadataClient.fetchModuleLatest(name, plugin.getApi().apiCompatibilityVersion(), moduleUpdateUrls);
            File copyTo = new File(plugin.getModulesFolder(), metadata.fileName());

            sendMessage(sender, PlexUtils.messageComponent("updateDownloading", metadata.fileName()));
            plugin.getApi().scheduler().runAsync(() -> downloadAndInstall(sender, metadata, copyTo, onSuccess));
        }
        catch (UpdateMetadataClient.MetadataException e)
        {
            sendMessage(sender, updateMetadataErrorComponent(e));
            if (!e.notFound())
            {
                PlexLog.error("Unable to update {0}: {1}", name, e.getMessage());
            }
        }
    }

    private synchronized ArtifactMetadata fetchLatestPlexMetadata(boolean useCache, String runningMinecraftVersion) throws UpdateMetadataClient.MetadataException
    {
        if (useCache)
        {
            if (latestPlexMetadata != null)
            {
                return latestPlexMetadata;
            }
            if (latestPlexMetadataFailure != null && isPlexMetadataFailureCacheFresh(latestPlexMetadataFailureAtMillis))
            {
                throw latestPlexMetadataFailure;
            }
        }

        try
        {
            latestPlexMetadata = metadataClient.fetchPlexLatest(runningMinecraftVersion);
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

    private synchronized ArtifactMetadata fetchPlexChannelLatestMetadata(boolean useCache) throws UpdateMetadataClient.MetadataException
    {
        if (useCache)
        {
            if (latestPlexChannelLatestMetadata != null)
            {
                return latestPlexChannelLatestMetadata;
            }
            if (latestPlexChannelLatestMetadataFailure != null && isPlexMetadataFailureCacheFresh(latestPlexChannelLatestMetadataFailureAtMillis))
            {
                throw latestPlexChannelLatestMetadataFailure;
            }
        }

        try
        {
            latestPlexChannelLatestMetadata = metadataClient.fetchPlexChannelLatest();
            latestPlexChannelLatestMetadataFailure = null;
            latestPlexChannelLatestMetadataFailureAtMillis = 0L;
            return latestPlexChannelLatestMetadata;
        }
        catch (UpdateMetadataClient.MetadataException e)
        {
            if (latestPlexChannelLatestMetadata == null)
            {
                latestPlexChannelLatestMetadataFailure = e;
                latestPlexChannelLatestMetadataFailureAtMillis = System.currentTimeMillis();
            }
            throw e;
        }
    }

    private List<String> sortedSupportedVersions(ArtifactMetadata metadata)
    {
        List<String> supportedVersions = new ArrayList<>(metadata.minecraftVersions());
        supportedVersions.sort(UpdateChecker::compareMinecraftVersions);
        return supportedVersions;
    }

    private String firstSupportedVersion(List<String> supportedVersions)
    {
        return supportedVersions.isEmpty() ? "unknown" : supportedVersions.get(0);
    }

    private String lastSupportedVersion(List<String> supportedVersions)
    {
        return supportedVersions.isEmpty() ? "unknown" : supportedVersions.get(supportedVersions.size() - 1);
    }

    private static int compareMinecraftVersions(String left, String right)
    {
        String[] leftParts = left.split("\\.");
        String[] rightParts = right.split("\\.");
        int length = Math.max(leftParts.length, rightParts.length);
        for (int i = 0; i < length; i++)
        {
            int leftPart = i < leftParts.length ? parseVersionPart(leftParts[i]) : 0;
            int rightPart = i < rightParts.length ? parseVersionPart(rightParts[i]) : 0;
            if (leftPart != rightPart)
            {
                return Integer.compare(leftPart, rightPart);
            }
        }
        return 0;
    }

    private static int parseVersionPart(String part)
    {
        try
        {
            return Integer.parseInt(part);
        }
        catch (NumberFormatException ignored)
        {
            return -1;
        }
    }

    private boolean isPlexMetadataFailureCacheFresh(long failureAtMillis)
    {
        return System.currentTimeMillis() - failureAtMillis < PLEX_METADATA_FAILURE_CACHE_MILLIS;
    }

    private String getRunningMinecraftVersion()
    {
        String version = Bukkit.getBukkitVersion();
        int buildIndex = version.indexOf(".build");
        if (buildIndex >= 0)
        {
            return version.substring(0, buildIndex);
        }

        int separatorIndex = version.indexOf('-');
        if (separatorIndex >= 0)
        {
            return version.substring(0, separatorIndex);
        }

        String buildMinecraftVersion = BuildInfo.getMinecraftVersion();
        int rangeIndex = buildMinecraftVersion.indexOf(" - ");
        if (rangeIndex >= 0)
        {
            return buildMinecraftVersion.substring(0, rangeIndex);
        }
        int listIndex = buildMinecraftVersion.indexOf(',');
        if (listIndex >= 0)
        {
            return buildMinecraftVersion.substring(0, listIndex);
        }
        return buildMinecraftVersion;
    }

    private boolean downloadAndInstall(CommandSender sender, ArtifactMetadata metadata, File copyTo, Runnable onSuccess)
    {
        File parent = copyTo.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs())
        {
            sendMessage(sender, PlexUtils.messageComponent("updateDirectoryFailed", parent.getAbsolutePath()));
            return false;
        }

        if (parent == null)
        {
            sendMessage(sender, PlexUtils.messageComponent("updateDownloadFailed", metadata.name()));
            PlexLog.error("Unable to download update {0}: destination has no parent directory", metadata.name());
            return false;
        }

        Path normalizedParent = parent.toPath().toAbsolutePath().normalize();
        Path normalizedDestination = copyTo.toPath().toAbsolutePath().normalize();
        if (!normalizedParent.equals(normalizedDestination.getParent()))
        {
            sendMessage(sender, PlexUtils.messageComponent("updateDownloadFailed", metadata.name()));
            PlexLog.error("Unable to download update {0}: artifact filename escapes the destination directory", metadata.name());
            return false;
        }

        Path temporaryPath = null;
        try
        {
            temporaryPath = Files.createTempFile(normalizedParent, metadata.fileName() + ".", ".download");
            download(metadata.downloadUrl(), temporaryPath.toFile());
            validateDownloadedFile(metadata, temporaryPath.toFile());
            Files.move(temporaryPath, normalizedDestination, StandardCopyOption.REPLACE_EXISTING);
            sendMessage(sender, PlexUtils.messageComponent("updateDownloaded"));
            if (onSuccess != null)
            {
                plugin.getApi().scheduler().runGlobal(onSuccess);
            }
            return true;
        }
        catch (IOException e)
        {
            sendMessage(sender, PlexUtils.messageComponent("updateDownloadFailed", metadata.name()));
            PlexLog.error("Unable to download update {0}: {1}", metadata.name(), e.getMessage());
            return false;
        }
        finally
        {
            if (temporaryPath != null)
            {
                try
                {
                    Files.deleteIfExists(temporaryPath);
                }
                catch (IOException ignored)
                {
                }
            }
        }
    }

    private void download(String downloadUrl, File destination) throws IOException
    {
        URI uri;
        try
        {
            uri = URI.create(downloadUrl);
        }
        catch (IllegalArgumentException exception)
        {
            throw new IOException("download URL is invalid", exception);
        }
        if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null)
        {
            throw new IOException("download URL must be an absolute HTTPS URL");
        }
        HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
        try
        {
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);

            int statusCode = connection.getResponseCode();
            if (!"https".equalsIgnoreCase(connection.getURL().getProtocol()))
            {
                throw new IOException("download request redirected to a non-HTTPS URL");
            }
            if (statusCode != HttpURLConnection.HTTP_OK)
            {
                throw new IOException("download request returned HTTP " + statusCode);
            }
            long contentLength = connection.getContentLengthLong();
            if (contentLength > MAX_ARTIFACT_BYTES)
            {
                throw new IOException("download exceeded the maximum artifact size");
            }

            try (InputStream inputStream = connection.getInputStream();
                 OutputStream outputStream = Files.newOutputStream(destination.toPath()))
            {
                byte[] buffer = new byte[8192];
                long total = 0L;
                int read;
                while ((read = inputStream.read(buffer)) != -1)
                {
                    total += read;
                    if (total > MAX_ARTIFACT_BYTES)
                    {
                        throw new IOException("download exceeded the maximum artifact size");
                    }
                    outputStream.write(buffer, 0, read);
                }
            }
        }
        finally
        {
            connection.disconnect();
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
