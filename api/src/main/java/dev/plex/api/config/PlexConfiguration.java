package dev.plex.api.config;

import java.util.List;

/**
 * Stable configuration wrapper exposed through the Plex module API.
 */
public interface PlexConfiguration
{
    /**
     * Reads a string value.
     *
     * @param path configuration path
     * @return configured string, or {@code null} if absent
     */
    String getString(String path);

    /**
     * Reads a string value with a fallback.
     *
     * @param path configuration path
     * @param fallback value returned when the path is absent
     * @return configured string or fallback
     */
    String getString(String path, String fallback);

    /**
     * Reads a boolean value.
     *
     * @param path configuration path
     * @return configured boolean, or the underlying configuration default
     */
    boolean getBoolean(String path);

    /**
     * Reads a boolean value with a fallback.
     *
     * @param path configuration path
     * @param fallback value returned when the path is absent
     * @return configured boolean or fallback
     */
    boolean getBoolean(String path, boolean fallback);

    /**
     * Reads an integer value.
     *
     * @param path configuration path
     * @return configured integer, or the underlying configuration default
     */
    int getInt(String path);

    /**
     * Reads an integer value with a fallback.
     *
     * @param path configuration path
     * @param fallback value returned when the path is absent
     * @return configured integer or fallback
     */
    int getInt(String path, int fallback);

    /**
     * Reads a string list.
     *
     * @param path configuration path
     * @return configured string list
     */
    List<String> getStringList(String path);

    /**
     * Reads a string list with a fallback.
     *
     * @param path configuration path
     * @param fallback value returned when the path is absent
     * @return configured string list or fallback
     */
    List<String> getStringList(String path, List<String> fallback);

    /**
     * Sets a configuration value.
     *
     * @param path configuration path
     * @param value value to write
     */
    void set(String path, Object value);

    /**
     * Sets comments above a configuration path.
     *
     * @param path configuration path
     * @param comments comments to write
     */
    void setComments(String path, List<String> comments);

    /**
     * Saves pending configuration changes to disk.
     */
    void save();
}
