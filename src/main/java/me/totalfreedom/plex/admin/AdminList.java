package me.totalfreedom.plex.admin;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import dev.morphia.Datastore;
import dev.morphia.query.Query;
import dev.morphia.query.experimental.filters.Filters;
import dev.morphia.query.internal.MorphiaCursor;
import me.totalfreedom.plex.Plex;
import me.totalfreedom.plex.player.PlexPlayer;
import me.totalfreedom.plex.rank.enums.Rank;
import me.totalfreedom.plex.storage.StorageType;

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
        if (Plex.get().getStorageType() == StorageType.MONGO)
        {
            Datastore store = Plex.get().getMongoConnection().getDatastore();
            Query<PlexPlayer> query = store.find(PlexPlayer.class);
            MorphiaCursor<PlexPlayer> cursor = query.iterator();
            while (cursor.hasNext())
            {
                PlexPlayer player = cursor.next();
                if (player.getRankFromString().isAtLeast(Rank.ADMIN))
                {
                    admins.add(player.getName());
                }
            }
        } else {
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

            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
        return admins;
    }

}
