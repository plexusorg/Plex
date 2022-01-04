package dev.plex.admin;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dev.morphia.Datastore;
import dev.morphia.query.Query;
import dev.plex.Plex;
import dev.plex.player.PlexPlayer;
import dev.plex.rank.enums.Rank;
import dev.plex.storage.StorageType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class AdminList
{

    private final Map<UUID, Admin> admins = Maps.newHashMap();

    public void addToCache(Admin admin)
    {
        admins.put(admin.getUuid(), admin);
    }

    public void removeFromCache(UUID uuid)
    {
        admins.remove(uuid);
    }

    public List<String> getAllAdmins()
    {
        List<String> admins = Lists.newArrayList();
        if (Plex.get().getStorageType() == StorageType.MONGODB)
        {
            Datastore store = Plex.get().getMongoConnection().getDatastore();
            Query<PlexPlayer> query = store.find(PlexPlayer.class);
            for (PlexPlayer player : query)
            {
                if (player.getRankFromString().isAtLeast(Rank.ADMIN))
                {
                    admins.add(player.getName());
                }
            }
        }
        else
        {
            try (Connection con = Plex.get().getSqlConnection().getCon())
            {
                PreparedStatement statement = con.prepareStatement("SELECT * FROM `players` WHERE rank IN(?, ?, ?)");
                statement.setString(1, Rank.ADMIN.name().toLowerCase());
                statement.setString(2, Rank.SENIOR_ADMIN.name().toLowerCase());
                statement.setString(3, Rank.EXECUTIVE.name().toLowerCase());

                ResultSet set = statement.executeQuery();
                while (set.next())
                {
                    admins.add(set.getString("name"));
                }

            }
            catch (SQLException throwables)
            {
                throwables.printStackTrace();
            }
        }
        return admins;
    }

}
