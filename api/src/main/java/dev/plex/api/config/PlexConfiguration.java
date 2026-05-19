package dev.plex.api.config;

import java.util.List;

/**
 * Stable configuration wrapper exposed through the Plex module API.
 */
public interface PlexConfiguration
{
    String getString(String path);

    String getString(String path, String fallback);

    boolean getBoolean(String path);

    boolean getBoolean(String path, boolean fallback);

    int getInt(String path);

    int getInt(String path, int fallback);

    List<String> getStringList(String path);

    List<String> getStringList(String path, List<String> fallback);

    void set(String path, Object value);

    void setComments(String path, List<String> comments);

    void save();
}
