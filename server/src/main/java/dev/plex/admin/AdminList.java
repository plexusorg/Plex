package dev.plex.admin;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import dev.morphia.Datastore;
import dev.morphia.query.Query;
import dev.plex.PlexBase;
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
import java.util.stream.Collectors;

/**
 * Cached storage for Admin objects
 *
 * @see Admin
 */

public class AdminList implements PlexBase
{
    /**
     * Key / Value storage, where the key is the unique ID of the admin
     */
    private final Map<UUID, Admin> admins = Maps.newHashMap();

    /**
     * Adds the admin to cache
     *
     * @param admin The admin object
     */
    public void addToCache(Admin admin)
    {
        admins.put(admin.getUuid(), admin);
    }

    /**
     * Removes an admin from the cache
     *
     * @param uuid The unique ID of the admin
     * @see UUID
     */
    public void removeFromCache(UUID uuid)
    {
        admins.remove(uuid);
    }

    /**
     * Gathers every admins username (cached and in the database)
     *
     * @return An array list of the names of every admin
     */
    public List<String> getAllAdmins()
    {
        List<String> admins = Lists.newArrayList();
        if (plugin.getStorageType() == StorageType.MONGODB)
        {
            Datastore store = plugin.getMongoConnection().getDatastore();
            Query<PlexPlayer> query = store.find(PlexPlayer.class);
            admins.addAll(query.stream().filter(plexPlayer -> plexPlayer.getRankFromString().isAtLeast(Rank.ADMIN) && plexPlayer.isAdminActive()).map(PlexPlayer::getName).toList());
        }
        else
        {
            try (Connection con = plugin.getSqlConnection().getCon())
            {
                PreparedStatement statement = con.prepareStatement("SELECT * FROM `players` WHERE rank IN(?, ?, ?) AND adminActive=true");
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

    /**
     * Gathers every admin (cached and in the database)
     *
     * @return An array list of the names of every admin
     */
    public List<PlexPlayer> getAllAdminPlayers()
    {
        List<PlexPlayer> plexPlayers = Lists.newArrayList();
        if (plugin.getStorageType() == StorageType.MONGODB)
        {
            Datastore store = plugin.getMongoConnection().getDatastore();
            Query<PlexPlayer> query = store.find(PlexPlayer.class);
            return query.stream().toList().stream().filter(player -> plugin.getRankManager().isAdmin(player)).collect(Collectors.toList());
        }
        else
        {
            try (Connection con = plugin.getSqlConnection().getCon())
            {
                PreparedStatement statement = con.prepareStatement("SELECT * FROM `players` WHERE rank IN(?, ?, ?) AND adminActive=true");
                statement.setString(1, Rank.ADMIN.name().toLowerCase());
                statement.setString(2, Rank.SENIOR_ADMIN.name().toLowerCase());
                statement.setString(3, Rank.EXECUTIVE.name().toLowerCase());

                ResultSet set = statement.executeQuery();
                while (set.next())
                {
                    String uuid = set.getString("uuid");
                    String name = set.getString("name");
                    String loginMSG = set.getString("login_msg");
                    String prefix = set.getString("prefix");
                    String rankName = set.getString("rank").toUpperCase();
                    long coins = set.getLong("coins");
                    boolean vanished = set.getBoolean("vanished");
                    boolean commandspy = set.getBoolean("commandspy");
                    List<String> ips = new Gson().fromJson(set.getString("ips"), new TypeToken<List<String>>()
                    {
                    }.getType());

                    PlexPlayer plexPlayer = new PlexPlayer(UUID.fromString(uuid));
                    plexPlayer.setName(name);
                    plexPlayer.setLoginMessage(loginMSG);
                    plexPlayer.setPrefix(prefix);
                    plexPlayer.setRank(rankName);
                    plexPlayer.setIps(ips);
                    plexPlayer.setCoins(coins);
                    plexPlayer.setVanished(vanished);
                    plexPlayer.setCommandSpy(commandspy);
                    plexPlayers.add(plexPlayer);
                }
            }
            catch (SQLException throwables)
            {
                throwables.printStackTrace();
            }
        }
        return plexPlayers;
    }
}
