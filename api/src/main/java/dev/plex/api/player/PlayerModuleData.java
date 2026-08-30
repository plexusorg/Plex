package dev.plex.api.player;

import com.google.gson.JsonElement;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Stores JSON data for one player and one module.
 *
 * <p>A key must start with a lowercase letter. It can contain lowercase
 * letters, digits, and underscores. Its maximum length is 64 characters.</p>
 */
public interface PlayerModuleData
{
    /**
     * Gets a raw JSON value.
     *
     * @param key data key
     * @return future containing the stored JSON value, if present
     */
    CompletableFuture<Optional<JsonElement>> get(String key);

    /**
     * Gets and maps a JSON value to a Java type.
     *
     * @param key data key
     * @param type target type
     * @param <T> target type
     * @return future containing the mapped value, or an empty result if the value is absent or has a different type
     */
    <T> CompletableFuture<Optional<T>> get(String key, Class<T> type);

    /**
     * Gets a string value.
     *
     * @param key data key
     * @param fallback value returned when the key is absent or incompatible
     * @return future containing the stored string or fallback
     */
    CompletableFuture<String> getString(String key, String fallback);

    /**
     * Gets a long value.
     *
     * @param key data key
     * @param fallback value returned when the key is absent or incompatible
     * @return future containing the stored long or fallback
     */
    CompletableFuture<Long> getLong(String key, long fallback);

    /**
     * Gets a boolean value.
     *
     * @param key data key
     * @param fallback value returned when the key is absent or incompatible
     * @return future containing the stored boolean or fallback
     */
    CompletableFuture<Boolean> getBoolean(String key, boolean fallback);

    /**
     * Stores a raw JSON value.
     *
     * @param key data key
     * @param value JSON value to store
     * @return completion future
     */
    CompletableFuture<Void> set(String key, JsonElement value);

    /**
     * Stores a Java value as JSON.
     *
     * @param key data key
     * @param value value to convert to JSON and store
     * @return completion future
     */
    CompletableFuture<Void> set(String key, Object value);

    /**
     * Removes a stored value.
     *
     * @param key data key
     * @return completion future
     */
    CompletableFuture<Void> remove(String key);
}
