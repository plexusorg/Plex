package dev.plex.api.impl;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import dev.plex.api.player.PlayerModuleData;
import dev.plex.storage.player.PlayerModuleDataRepository;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class DefaultPlayerModuleData implements PlayerModuleData
{
    private static final Gson GSON = new Gson();
    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{0,63}$");

    private final PlayerModuleDataRepository repository;
    private final Executor executor;
    private final String modulePrefix;
    private final UUID playerUuid;

    public DefaultPlayerModuleData(PlayerModuleDataRepository repository, Executor executor, String modulePrefix, UUID playerUuid)
    {
        this.repository = repository;
        this.executor = executor;
        this.modulePrefix = modulePrefix;
        this.playerUuid = playerUuid;
    }

    @Override
    public CompletableFuture<Optional<JsonElement>> get(String key)
    {
        return read(() -> repository.get(playerUuid, modulePrefix, validateKey(key)));
    }

    @Override
    public <T> CompletableFuture<Optional<T>> get(String key, Class<T> type)
    {
        return read(() ->
        {
            Objects.requireNonNull(type, "type");
            Optional<JsonElement> value = repository.get(playerUuid, modulePrefix, validateKey(key));
            try
            {
                return value.map(element -> GSON.fromJson(element, type));
            }
            catch (JsonParseException | ClassCastException ex)
            {
                return Optional.empty();
            }
        });
    }

    @Override
    public CompletableFuture<String> getString(String key, String fallback)
    {
        return get(key)
                .thenApply(value -> value.filter(JsonElement::isJsonPrimitive)
                        .map(JsonElement::getAsJsonPrimitive)
                        .filter(primitive -> primitive.isString())
                        .map(primitive -> primitive.getAsString())
                        .orElse(fallback));
    }

    @Override
    public CompletableFuture<Long> getLong(String key, long fallback)
    {
        return get(key)
                .thenApply(value -> value.filter(JsonElement::isJsonPrimitive)
                        .map(JsonElement::getAsJsonPrimitive)
                        .filter(primitive -> primitive.isNumber())
                        .map(primitive -> primitive.getAsLong())
                        .orElse(fallback));
    }

    @Override
    public CompletableFuture<Boolean> getBoolean(String key, boolean fallback)
    {
        return get(key)
                .thenApply(value -> value.filter(JsonElement::isJsonPrimitive)
                        .map(JsonElement::getAsJsonPrimitive)
                        .filter(primitive -> primitive.isBoolean())
                        .map(primitive -> primitive.getAsBoolean())
                        .orElse(fallback));
    }

    @Override
    public CompletableFuture<Void> set(String key, JsonElement value)
    {
        return write(() -> repository.set(playerUuid, modulePrefix, validateKey(key), Objects.requireNonNull(value, "value")));
    }

    @Override
    public CompletableFuture<Void> set(String key, Object value)
    {
        return write(() -> repository.set(playerUuid, modulePrefix, validateKey(key), GSON.toJsonTree(value)));
    }

    @Override
    public CompletableFuture<Void> remove(String key)
    {
        return write(() -> repository.remove(playerUuid, modulePrefix, validateKey(key)));
    }

    private <T> CompletableFuture<T> read(Supplier<T> action)
    {
        try
        {
            return CompletableFuture.supplyAsync(action, executor);
        }
        catch (RuntimeException failure)
        {
            return CompletableFuture.failedFuture(failure);
        }
    }

    private CompletableFuture<Void> write(Runnable action)
    {
        try
        {
            return CompletableFuture.runAsync(action, executor);
        }
        catch (RuntimeException failure)
        {
            return CompletableFuture.failedFuture(failure);
        }
    }

    private String validateKey(String key)
    {
        if (key == null || !KEY_PATTERN.matcher(key).matches())
        {
            throw new IllegalArgumentException("Invalid player module data key: " + key);
        }
        return key;
    }
}
