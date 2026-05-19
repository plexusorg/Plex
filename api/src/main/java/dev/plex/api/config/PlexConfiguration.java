package dev.plex.api.config;

import java.util.List;

/**
 * Stable configuration wrapper exposed through the Plex module API.
 */
public interface PlexConfiguration
{
    String getString(String path);

    boolean getBoolean(String path);

    int getInt(String path);

    List<String> getStringList(String path);

    void set(String path, Object value);

    void setComments(String path, List<String> comments);

    void save();
}
