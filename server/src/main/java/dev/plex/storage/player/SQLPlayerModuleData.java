package dev.plex.storage.player;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import dev.plex.storage.StorageType;
import dev.plex.util.PlexLog;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.JdbiException;

import java.util.Optional;
import java.util.UUID;

public class SQLPlayerModuleData implements PlayerModuleDataRepository
{
    private final Jdbi jdbi;
    private final StorageType storageType;

    public SQLPlayerModuleData(Jdbi jdbi, StorageType storageType)
    {
        this.jdbi = jdbi;
        this.storageType = storageType;
    }

    @Override
    public Optional<JsonElement> get(UUID playerUuid, String module, String key)
    {
        try
        {
            return jdbi.withHandle(h -> h.createQuery(
                            "SELECT value_json FROM player_module_data WHERE player_uuid = :p AND module = :m AND data_key = :k")
                    .bind("p", playerUuid.toString())
                    .bind("m", module)
                    .bind("k", key)
                    .mapTo(String.class).findFirst())
                    .map(JsonParser::parseString);
        }
        catch (JdbiException | JsonSyntaxException e)
        {
            PlexLog.warn("Failed to load player module data {0}/{1}/{2}: {3}", playerUuid, module, key, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public void set(UUID playerUuid, String module, String key, JsonElement value)
    {
        try
        {
            jdbi.useHandle(h -> h.createUpdate(storageType.playerModuleDataUpsertSql())
                    .bind(0, playerUuid.toString())
                    .bind(1, module)
                    .bind(2, key)
                    .bind(3, value.toString())
                    .bind(4, System.currentTimeMillis())
                    .execute());
        }
        catch (JdbiException e)
        {
            PlexLog.warn("Failed to save player module data {0}/{1}/{2}: {3}", playerUuid, module, key, e.getMessage());
        }
    }

    @Override
    public void remove(UUID playerUuid, String module, String key)
    {
        try
        {
            jdbi.useHandle(h -> h.createUpdate(
                            "DELETE FROM player_module_data WHERE player_uuid = :p AND module = :m AND data_key = :k")
                    .bind("p", playerUuid.toString())
                    .bind("m", module)
                    .bind("k", key)
                    .execute());
        }
        catch (JdbiException e)
        {
            PlexLog.warn("Failed to remove player module data {0}/{1}/{2}: {3}", playerUuid, module, key, e.getMessage());
        }
    }
}
