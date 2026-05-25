package dev.plex.storage.player;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import dev.plex.storage.SQLConnection;
import dev.plex.storage.StorageType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public class SQLPlayerModuleData implements PlayerModuleDataRepository
{
    private final SQLConnection sqlConnection;
    private final StorageType storageType;

    public SQLPlayerModuleData(SQLConnection sqlConnection, StorageType storageType)
    {
        this.sqlConnection = sqlConnection;
        this.storageType = storageType;
    }

    @Override
    public Optional<JsonElement> get(UUID playerUuid, String module, String key)
    {
        String sql = "SELECT value_json FROM player_module_data WHERE player_uuid = ? AND module = ? AND data_key = ?";
        try (Connection connection = sqlConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, playerUuid.toString());
            statement.setString(2, module);
            statement.setString(3, key);
            try (ResultSet resultSet = statement.executeQuery())
            {
                if (!resultSet.next())
                {
                    return Optional.empty();
                }
                return Optional.of(JsonParser.parseString(resultSet.getString("value_json")));
            }
        }
        catch (SQLException | JsonSyntaxException e)
        {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    @Override
    public void set(UUID playerUuid, String module, String key, JsonElement value)
    {
        try (Connection connection = sqlConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(storageType.playerModuleDataUpsertSql()))
        {
            statement.setString(1, playerUuid.toString());
            statement.setString(2, module);
            statement.setString(3, key);
            statement.setString(4, value.toString());
            statement.setLong(5, System.currentTimeMillis());
            statement.executeUpdate();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void remove(UUID playerUuid, String module, String key)
    {
        String sql = "DELETE FROM player_module_data WHERE player_uuid = ? AND module = ? AND data_key = ?";
        try (Connection connection = sqlConnection.getConnection(); PreparedStatement statement = connection.prepareStatement(sql))
        {
            statement.setString(1, playerUuid.toString());
            statement.setString(2, module);
            statement.setString(3, key);
            statement.executeUpdate();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }
}
