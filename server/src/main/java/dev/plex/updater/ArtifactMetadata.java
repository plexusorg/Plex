package dev.plex.updater;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Pattern;

public final class ArtifactMetadata
{
    private static final int CURRENT_SCHEMA_VERSION = 1;
    private static final Pattern SHA256_PATTERN = Pattern.compile("^[a-fA-F0-9]{64}$");

    private int schemaVersion;
    private String name;
    private String version;
    private String buildNumber;
    private String commit;
    private String channel;
    private List<String> minecraftVersions;
    private Integer apiCompatibility;
    private Integer requiredApiCompatibility;
    private String downloadUrl;
    private String sha256;
    private Long size;
    private String publishedAt;

    public String name()
    {
        return name;
    }

    public String version()
    {
        return version;
    }

    public String buildNumber()
    {
        return buildNumber;
    }

    public String commit()
    {
        return commit;
    }

    public String channel()
    {
        return channel;
    }

    public String downloadUrl()
    {
        return downloadUrl;
    }

    public String sha256()
    {
        return sha256;
    }

    public Long size()
    {
        return size;
    }

    public String publishedAt()
    {
        return publishedAt;
    }

    public List<String> minecraftVersions()
    {
        return minecraftVersions == null ? List.of() : List.copyOf(minecraftVersions);
    }

    public Optional<String> validatePlex(UpdateChannel requestedChannel)
    {
        Optional<String> commonError = validateCommon(requestedChannel);
        if (commonError.isPresent())
        {
            return commonError;
        }
        if (!"Plex".equalsIgnoreCase(name))
        {
            return Optional.of("Plex metadata has unexpected artifact name " + name);
        }
        if (minecraftVersions == null || minecraftVersions.isEmpty())
        {
            return Optional.of("Plex metadata is missing minecraftVersions");
        }
        if (apiCompatibility == null)
        {
            return Optional.of("Plex metadata is missing apiCompatibility");
        }
        return Optional.empty();
    }

    public Optional<String> validatePlex(UpdateChannel requestedChannel, String minecraftVersion)
    {
        Optional<String> plexError = validatePlex(requestedChannel);
        if (plexError.isPresent())
        {
            return plexError;
        }
        if (!supportsMinecraftVersion(minecraftVersion))
        {
            return Optional.of("metadata does not include Minecraft version " + minecraftVersion);
        }
        return Optional.empty();
    }

    public Optional<String> validateModule(UpdateChannel requestedChannel, String moduleName, int apiCompatibility)
    {
        Optional<String> commonError = validateCommon(requestedChannel);
        if (commonError.isPresent())
        {
            return commonError;
        }
        if (!moduleName.equalsIgnoreCase(name))
        {
            return Optional.of("module metadata has unexpected artifact name " + name);
        }
        if (requiredApiCompatibility == null)
        {
            return Optional.of("module metadata is missing requiredApiCompatibility");
        }
        if (requiredApiCompatibility != apiCompatibility)
        {
            return Optional.of("module metadata requires API compatibility " + requiredApiCompatibility + ", but Plex provides " + apiCompatibility);
        }
        return Optional.empty();
    }

    public boolean matchesCurrentBuild(String currentVersion, String currentBuildNumber, String currentCommit)
    {
        if (!Objects.equals(version, currentVersion))
        {
            return false;
        }
        if (isKnown(commit) && isKnown(currentCommit))
        {
            return commit.equalsIgnoreCase(currentCommit);
        }
        return Objects.equals(buildNumber, currentBuildNumber);
    }

    public String fileName()
    {
        try
        {
            String path = URI.create(downloadUrl).getPath();
            int separatorIndex = path.lastIndexOf('/');
            String fileName = separatorIndex >= 0 ? path.substring(separatorIndex + 1) : path;
            if (!fileName.isBlank())
            {
                return URLDecoder.decode(fileName, StandardCharsets.UTF_8);
            }
        }
        catch (IllegalArgumentException ignored)
        {
        }
        return name + "-" + version + ".jar";
    }

    private Optional<String> validateCommon(UpdateChannel requestedChannel)
    {
        if (schemaVersion != CURRENT_SCHEMA_VERSION)
        {
            return Optional.of("metadata schemaVersion " + schemaVersion + " is not supported");
        }
        if (isBlank(name))
        {
            return Optional.of("metadata is missing name");
        }
        if (isBlank(version))
        {
            return Optional.of("metadata is missing version");
        }
        if (isBlank(channel))
        {
            return Optional.of("metadata is missing channel");
        }
        if (!requestedChannel.id().equalsIgnoreCase(channel))
        {
            return Optional.of("metadata channel " + channel + " does not match requested channel " + requestedChannel.id());
        }
        if (requestedChannel == UpdateChannel.STABLE && version.toUpperCase(Locale.ROOT).contains("SNAPSHOT"))
        {
            return Optional.of("stable metadata must not point to a SNAPSHOT version");
        }
        if (isBlank(downloadUrl))
        {
            return Optional.of("metadata is missing downloadUrl");
        }
        if (isBlank(sha256) || !SHA256_PATTERN.matcher(sha256).matches())
        {
            return Optional.of("metadata is missing a valid sha256");
        }
        return Optional.empty();
    }

    private boolean supportsMinecraftVersion(String minecraftVersion)
    {
        return minecraftVersions != null && minecraftVersions.contains(minecraftVersion);
    }

    private static boolean isBlank(String value)
    {
        return value == null || value.isBlank();
    }

    private static boolean isKnown(String value)
    {
        return !isBlank(value) && !"unknown".equalsIgnoreCase(value);
    }
}
