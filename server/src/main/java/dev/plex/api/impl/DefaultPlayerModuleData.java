package dev.plex.api.impl;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import dev.plex.api.player.PlayerModuleData;
import dev.plex.storage.player.PlayerModuleDataRepository;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

public class DefaultPlayerModuleData implements PlayerModuleData
{
    private static final Gson GSON = new Gson();
    private static final Pattern KEY_PATTERN = Pattern.compile("^[a-z][a-z0-9_]{0,63}$");

    private final PlayerModuleDataRepository repository;
    private final String modulePrefix;
    private final UUID playerUuid;

    public DefaultPlayerModuleData(PlayerModuleDataRepository repository, String modulePrefix, UUID playerUuid)
    {
        this.repository = repository;
        this.modulePrefix = modulePrefix;
        this.playerUuid = playerUuid;
    }

    @Override
    public Optional<JsonElement> get(String key)
    {
        return repository.get(playerUuid, modulePrefix, validateKey(key));
    }

    @Override
    public <T> Optional<T> get(String key, Class<T> type)
    {
        return get(key).map(element -> GSON.fromJson(element, type));
    }

    @Override
    public String getString(String key, String fallback)
    {
        return get(key)
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsJsonPrimitive)
                .filter(primitive -> primitive.isString())
                .map(primitive -> primitive.getAsString())
                .orElse(fallback);
    }

    @Override
    public long getLong(String key, long fallback)
    {
        return get(key)
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsJsonPrimitive)
                .filter(primitive -> primitive.isNumber())
                .map(primitive -> primitive.getAsLong())
                .orElse(fallback);
    }

    @Override
    public boolean getBoolean(String key, boolean fallback)
    {
        return get(key)
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsJsonPrimitive)
                .filter(primitive -> primitive.isBoolean())
                .map(primitive -> primitive.getAsBoolean())
                .orElse(fallback);
    }

    @Override
    public void set(String key, JsonElement value)
    {
        repository.set(playerUuid, modulePrefix, validateKey(key), Objects.requireNonNull(value, "value"));
    }

    @Override
    public void set(String key, Object value)
    {
        set(key, GSON.toJsonTree(value));
    }

    @Override
    public void remove(String key)
    {
        repository.remove(playerUuid, modulePrefix, validateKey(key));
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
