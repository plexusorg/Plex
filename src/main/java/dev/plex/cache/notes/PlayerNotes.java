package dev.plex.cache.notes;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import dev.plex.Plex;
import dev.plex.cache.player.PlayerCache;
import dev.plex.player.PlexPlayer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class PlayerNotes
{
    private final String SELECT = "SELECT * FROM `notes` WHERE uuid=?";
    //private final String UPDATE = "UPDATE `notes` SET name=?, written_by=?, note=? WHERE uuid=?";
    private final String INSERT = "INSERT INTO `notes` (`uuid`, `name`, `written_by`, `note`) VALUES (?, ?, ?, ?);";

    public PlexPlayer getByUUID(UUID uuid)
    {
        try (Connection con = Plex.get().getSqlConnection().getCon())
        {
            PreparedStatement statement = con.prepareStatement(SELECT);
            statement.setString(1, uuid.toString());
            ResultSet set = statement.executeQuery();
            PlexPlayer plexPlayer = new PlexPlayer(uuid);
            while (set.next())
            {
                String name = set.getString("name");
                String writtenBy = set.getString("written_by");
                String note = set.getString("note");

            }
            return plexPlayer;
        }
        catch (SQLException throwables)
        {
            throwables.printStackTrace();
        }
        return null;
    }
}
