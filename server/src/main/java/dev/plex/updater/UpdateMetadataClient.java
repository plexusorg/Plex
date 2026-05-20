package dev.plex.updater;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

public final class UpdateMetadataClient
{
    private static final List<String> DEFAULT_BASE_URLS = List.of("https://updater.plex.us.org", "https://plex-updater.com");

    private final Gson gson = new Gson();
    private final UpdateChannel channel;

    public UpdateMetadataClient(UpdateChannel channel)
    {
        this.channel = channel;
    }

    public ArtifactMetadata fetchPlexLatest(String minecraftVersion) throws MetadataException
    {
        String path = "/api/v1/projects/Plex/channels/" + channel.id() + "/latest/minecraft/" + encodePathSegment(minecraftVersion) + ".json";
        ArtifactMetadata metadata = fetch(path);
        Optional<String> validationError = metadata.validatePlex(channel, minecraftVersion);
        if (validationError.isPresent())
        {
            throw new MetadataException(validationError.get(), false);
        }
        return metadata;
    }

    public ArtifactMetadata fetchModuleLatest(String moduleName, int apiCompatibility) throws MetadataException
    {
        return fetchModuleLatest(moduleName, apiCompatibility, List.of());
    }

    public ArtifactMetadata fetchModuleLatest(String moduleName, int apiCompatibility, List<String> baseUrls) throws MetadataException
    {
        String path = "/api/v1/projects/" + encodePathSegment(moduleName) + "/channels/" + channel.id() + "/latest/api/" + apiCompatibility + ".json";
        ArtifactMetadata metadata = fetch(path, baseUrls);
        Optional<String> validationError = metadata.validateModule(channel, moduleName, apiCompatibility);
        if (validationError.isPresent())
        {
            throw new MetadataException(validationError.get(), false);
        }
        return metadata;
    }

    private ArtifactMetadata fetch(String path) throws MetadataException
    {
        return fetch(path, DEFAULT_BASE_URLS);
    }

    private ArtifactMetadata fetch(String path, List<String> baseUrls) throws MetadataException
    {
        MetadataException notFound = null;
        MetadataException failure = null;
        for (String baseUrl : normalizeBaseUrls(baseUrls))
        {
            try
            {
                return fetch(baseUrl, path);
            }
            catch (MetadataException e)
            {
                if (e.notFound())
                {
                    if (notFound == null)
                    {
                        notFound = e;
                    }
                    continue;
                }
                failure = e;
            }
        }

        if (failure != null)
        {
            throw new MetadataException("all metadata endpoints failed for " + path + "; last error: " + failure.getMessage(), false, failure);
        }
        if (notFound != null)
        {
            throw notFound;
        }
        throw new MetadataException("no updater metadata endpoints are available", false);
    }

    private ArtifactMetadata fetch(String baseUrl, String path) throws MetadataException
    {
        String url = baseUrl + path;
        try
        {
            HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("Accept", "application/json");

            int statusCode = connection.getResponseCode();
            if (statusCode == HttpURLConnection.HTTP_NOT_FOUND)
            {
                throw new MetadataException("no compatible update metadata exists at " + path + " on " + baseUrl, true);
            }
            if (statusCode != HttpURLConnection.HTTP_OK)
            {
                throw new MetadataException("metadata request returned HTTP " + statusCode + " for " + path + " on " + baseUrl, false);
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)))
            {
                ArtifactMetadata metadata = gson.fromJson(reader, ArtifactMetadata.class);
                if (metadata == null)
                {
                    throw new MetadataException("metadata response was empty for " + path + " on " + baseUrl, false);
                }
                return metadata;
            }
            catch (JsonSyntaxException e)
            {
                throw new MetadataException("metadata response was not valid JSON for " + path + " on " + baseUrl, false, e);
            }
        }
        catch (IllegalArgumentException e)
        {
            throw new MetadataException("metadata URL is invalid: " + url, false, e);
        }
        catch (IOException e)
        {
            throw new MetadataException("metadata request failed for " + path + " on " + baseUrl, false, e);
        }
    }

    private static String encodePathSegment(String value)
    {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static List<String> normalizeBaseUrls(List<String> baseUrls)
    {
        List<String> urls = baseUrls == null || baseUrls.isEmpty() ? DEFAULT_BASE_URLS : baseUrls;
        return urls.stream()
                .map(String::trim)
                .filter(url -> !url.isBlank())
                .map(url -> url.endsWith("/") ? url.substring(0, url.length() - 1) : url)
                .distinct()
                .toList();
    }

    public static final class MetadataException extends Exception
    {
        private final boolean notFound;

        private MetadataException(String message, boolean notFound)
        {
            super(message);
            this.notFound = notFound;
        }

        private MetadataException(String message, boolean notFound, Throwable cause)
        {
            super(message, cause);
            this.notFound = notFound;
        }

        public boolean notFound()
        {
            return notFound;
        }
    }
}
