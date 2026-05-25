package dev.plex.storage.player;

import com.google.gson.JsonElement;

import java.util.Optional;
import java.util.UUID;

public interface PlayerModuleDataRepository
{
    Optional<JsonElement> get(UUID playerUuid, String module, String key);

    void set(UUID playerUuid, String module, String key, JsonElement value);

    void remove(UUID playerUuid, String module, String key);
}
