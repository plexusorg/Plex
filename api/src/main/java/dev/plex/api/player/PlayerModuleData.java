package dev.plex.api.player;

import com.google.gson.JsonElement;
import java.util.Optional;

/**
 * Typed key-value JSON storage for a player's module data.
 */
public interface PlayerModuleData
{
    /**
     * Gets a raw JSON value.
     *
     * @param key data key
     * @return stored JSON value, if present
     */
    Optional<JsonElement> get(String key);

    /**
     * Gets and maps a JSON value to a Java type.
     *
     * @param key data key
     * @param type target type
     * @param <T> target type
     * @return mapped value, if present
     */
    <T> Optional<T> get(String key, Class<T> type);

    /**
     * Gets a string value.
     *
     * @param key data key
     * @param fallback value returned when the key is absent or incompatible
     * @return stored string or fallback
     */
    String getString(String key, String fallback);

    /**
     * Gets a long value.
     *
     * @param key data key
     * @param fallback value returned when the key is absent or incompatible
     * @return stored long or fallback
     */
    long getLong(String key, long fallback);

    /**
     * Gets a boolean value.
     *
     * @param key data key
     * @param fallback value returned when the key is absent or incompatible
     * @return stored boolean or fallback
     */
    boolean getBoolean(String key, boolean fallback);

    /**
     * Stores a raw JSON value.
     *
     * @param key data key
     * @param value JSON value to store
     */
    void set(String key, JsonElement value);

    /**
     * Stores a Java value as JSON.
     *
     * @param key data key
     * @param value value to serialize and store
     */
    void set(String key, Object value);

    /**
     * Removes a stored value.
     *
     * @param key data key
     */
    void remove(String key);
}
