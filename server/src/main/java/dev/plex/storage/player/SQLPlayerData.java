package dev.plex.storage.player;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import dev.plex.Plex;
import dev.plex.player.PlexPlayer;
import dev.plex.storage.StorageType;
import dev.plex.util.PlexLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * SQL fetching utilities for players
 */
public class SQLPlayerData
{
    private final String SELECT = "SELECT * FROM `players` WHERE uuid=?";
    private final String UPDATE = "UPDATE `players` SET name=?, login_msg=?, prefix=?, rank=?, adminActive=?, ips=?, coins=?, vanished=?, commandspy=? WHERE uuid=?";
    private final String INSERT = "INSERT INTO `players` (`uuid`, `name`, `login_msg`, `prefix`, `rank`, `adminActive`, `ips`, `coins`, `vanished`, `commandspy`) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";

    /**
     * Checks if a player exists in the SQL database
     *
     * @param uuid The unique ID of the player
     * @return true if the player was found in the database
     */
    public boolean exists(UUID uuid)
    {
        try (Connection con = Plex.get().getSqlConnection().getCon())
        {
            PreparedStatement statement = con.prepareStatement(SELECT);
            statement.setString(1, uuid.toString());
            ResultSet set = statement.executeQuery();
            return set.next();
        }
        catch (SQLException throwables)
        {
            throwables.printStackTrace();
        }
        return false;
    }

    public boolean exists(String username)
    {
        try (Connection con = Plex.get().getSqlConnection().getCon())
        {
            PreparedStatement statement = con.prepareStatement("SELECT * FROM `players` WHERE name=?");
            statement.setString(1, username);
            ResultSet set = statement.executeQuery();
            return set.next();
        }
        catch (SQLException throwables)
        {
            throwables.printStackTrace();
        }
        return false;
    }

    /**
     * Gets the player from cache or from the SQL database
     *
     * @param uuid The unique ID of the player
     * @return a PlexPlayer object
     * @see PlexPlayer
     */
    public PlexPlayer getByUUID(UUID uuid, boolean loadExtraData)
    {
        if (Plex.get().getPlayerCache().getPlexPlayerMap().containsKey(uuid))
        {
            return Plex.get().getPlayerCache().getPlexPlayerMap().get(uuid);
        }

        try (Connection con = Plex.get().getSqlConnection().getCon())
        {
            PreparedStatement statement = con.prepareStatement(SELECT);
            statement.setString(1, uuid.toString());
            ResultSet set = statement.executeQuery();
            PlexPlayer plexPlayer = new PlexPlayer(uuid, loadExtraData);
            while (set.next())
            {
                String name = set.getString("name");
                String loginMSG = set.getString("login_msg");
                String prefix = set.getString("prefix");
                String rankName = set.getString("rank").toUpperCase();
                boolean adminActive = set.getBoolean("adminActive");
                long coins = set.getLong("coins");
                boolean vanished = set.getBoolean("vanished");
                boolean commandspy = set.getBoolean("commandspy");
                List<String> ips = new Gson().fromJson(set.getString("ips"), new TypeToken<List<String>>()
                {
                }.getType());
                plexPlayer.setName(name);
                plexPlayer.setLoginMessage(loginMSG);
                plexPlayer.setPrefix(prefix);
                plexPlayer.setRank(rankName);
                plexPlayer.setAdminActive(adminActive);
                plexPlayer.setIps(ips);
                plexPlayer.setCoins(coins);
                plexPlayer.setVanished(vanished);
                plexPlayer.setCommandSpy(commandspy);
            }
            return plexPlayer;
        }
        catch (SQLException throwables)
        {
            throwables.printStackTrace();
        }
        return null;
    }

    public PlexPlayer getByUUID(UUID uuid)
    {
        return this.getByUUID(uuid, true);
    }

    public PlexPlayer getByName(String username, boolean loadExtraData)
    {
        PlexPlayer player = Plex.get().getPlayerCache().getPlexPlayerMap().values().stream().filter(plexPlayer -> plexPlayer.getName().equalsIgnoreCase(username)).findFirst().orElse(null);
        if (player != null)
        {
            return player;
        }
        try (Connection con = Plex.get().getSqlConnection().getCon())
        {
            PreparedStatement statement = con.prepareStatement("SELECT * FROM `players` WHERE name=? LIMIT 1");
            statement.setString(1, username);
            ResultSet set = statement.executeQuery();
            while (set.next())
            {
                PlexPlayer plexPlayer = new PlexPlayer(UUID.fromString(set.getString("uuid")), loadExtraData);
                String loginMSG = set.getString("login_msg");
                String prefix = set.getString("prefix");
                String rankName = set.getString("rank").toUpperCase();
                boolean adminActive = set.getBoolean("adminActive");
                long coins = set.getLong("coins");
                boolean vanished = set.getBoolean("vanished");
                boolean commandspy = set.getBoolean("commandspy");
                List<String> ips = new Gson().fromJson(set.getString("ips"), new TypeToken<List<String>>()
                {
                }.getType());
                plexPlayer.setName(username);
                plexPlayer.setLoginMessage(loginMSG);
                plexPlayer.setPrefix(prefix);
                plexPlayer.setRank(rankName);
                plexPlayer.setAdminActive(adminActive);
                plexPlayer.setIps(ips);
                plexPlayer.setCoins(coins);
                plexPlayer.setVanished(vanished);
                plexPlayer.setCommandSpy(commandspy);
                return plexPlayer;
            }
            return null;
        }
        catch (SQLException throwables)
        {
            throwables.printStackTrace();
        }
        return null;
    }

    public PlexPlayer getByName(String username)
    {
        return getByName(username, true);
    }

    /**
     * Gets the player from cache or from the SQL database
     *
     * @param ip The IP address of the player.
     * @return a PlexPlayer object
     * @see PlexPlayer
     */
    public PlexPlayer getByIP(String ip)
    {
        PlexPlayer player = Plex.get().getPlayerCache().getPlexPlayerMap().values().stream().filter(plexPlayer -> plexPlayer.getIps().contains(ip)).findFirst().orElse(null);
        if (player != null)
        {
            return player;
        }

        if (Plex.get().getStorageType() == StorageType.MARIADB)
        {
            try (Connection con = Plex.get().getSqlConnection().getCon())
            {
                PreparedStatement statement = con.prepareStatement("select * from `players` where json_search(ips, ?, ?) IS NOT NULL LIMIT 1");
                statement.setString(1, "one");
                statement.setString(2, ip);
                ResultSet set = statement.executeQuery();

                PlexPlayer plexPlayer = null;
                while (set.next())
                {
                    String uuid = set.getString("uuid");
                    String name = set.getString("name");
                    String loginMSG = set.getString("login_msg");
                    String prefix = set.getString("prefix");
                    String rankName = set.getString("rank").toUpperCase();
                    boolean adminActive = set.getBoolean("adminActive");
                    long coins = set.getLong("coins");
                    boolean vanished = set.getBoolean("vanished");
                    boolean commandspy = set.getBoolean("commandspy");
                    List<String> ips = new Gson().fromJson(set.getString("ips"), new TypeToken<List<String>>()
                    {
                    }.getType());
                    plexPlayer = new PlexPlayer(UUID.fromString(uuid));
                    plexPlayer.setName(name);
                    plexPlayer.setLoginMessage(loginMSG);
                    plexPlayer.setPrefix(prefix);
                    plexPlayer.setRank(rankName);
                    plexPlayer.setAdminActive(adminActive);
                    plexPlayer.setIps(ips);
                    plexPlayer.setCoins(coins);
                    plexPlayer.setVanished(vanished);
                    plexPlayer.setCommandSpy(commandspy);
                }
                return plexPlayer;
            }
            catch (SQLException throwables)
            {
                throwables.printStackTrace();
            }
        }
        else if (Plex.get().getStorageType() == StorageType.SQLITE)
        {
            PlexLog.warn("Querying a user by IP running SQLite can cause performance issues! Please try to switch to a remote DB ASAP!");
            try (Connection con = Plex.get().getSqlConnection().getCon())
            {
                PreparedStatement statement = con.prepareStatement("select * from `players`");
                ResultSet set = statement.executeQuery();

                PlexPlayer plexPlayer = null;
                while (set.next())
                {
                    List<String> ips = new Gson().fromJson(set.getString("ips"), new TypeToken<List<String>>()
                    {
                    }.getType());
                    if (!ips.contains(ip))
                    {
                        continue;
                    }
                    String uuid = set.getString("uuid");
                    String name = set.getString("name");
                    String loginMSG = set.getString("login_msg");
                    String prefix = set.getString("prefix");
                    String rankName = set.getString("rank").toUpperCase();
                    boolean adminActive = set.getBoolean("adminActive");
                    long coins = set.getLong("coins");
                    boolean vanished = set.getBoolean("vanished");
                    boolean commandspy = set.getBoolean("commandspy");

                    plexPlayer = new PlexPlayer(UUID.fromString(uuid));
                    plexPlayer.setName(name);
                    plexPlayer.setLoginMessage(loginMSG);
                    plexPlayer.setPrefix(prefix);
                    plexPlayer.setRank(rankName);
                    plexPlayer.setAdminActive(adminActive);
                    plexPlayer.setIps(ips);
                    plexPlayer.setCoins(coins);
                    plexPlayer.setVanished(vanished);
                    plexPlayer.setCommandSpy(commandspy);
                }
                return plexPlayer;
            }
            catch (SQLException throwables)
            {
                throwables.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Updates a player's information in the SQL database
     *
     * @param player The PlexPlayer object
     * @see PlexPlayer
     */
    public void update(PlexPlayer player)
    {
        try (Connection con = Plex.get().getSqlConnection().getCon())
        {
            PreparedStatement statement = con.prepareStatement(UPDATE);
            statement.setString(1, player.getName());
            statement.setString(2, player.getLoginMessage());
            statement.setString(3, player.getPrefix());
            statement.setString(4, player.getRank().toLowerCase());
            statement.setBoolean(5, player.isAdminActive());
            statement.setString(6, new Gson().toJson(player.getIps()));
            statement.setLong(7, player.getCoins());
            statement.setBoolean(8, player.isVanished());
            statement.setBoolean(9, player.isCommandSpy());
            statement.setString(10, player.getUuid().toString());
            statement.executeUpdate();
        }
        catch (SQLException throwables)
        {
            throwables.printStackTrace();
        }
    }

    /**
     * Inserts the player's information in the database
     *
     * @param player The PlexPlayer object
     * @see PlexPlayer
     */
    public void insert(PlexPlayer player)
    {
        try (Connection con = Plex.get().getSqlConnection().getCon())
        {
            PreparedStatement statement = con.prepareStatement(INSERT);
            statement.setString(1, player.getUuid().toString());
            statement.setString(2, player.getName());
            statement.setString(3, player.getLoginMessage());
            statement.setString(4, player.getPrefix());
            statement.setString(5, player.getRank().toLowerCase());
            statement.setBoolean(6, player.isAdminActive());
            statement.setString(7, new Gson().toJson(player.getIps()));
            statement.setLong(8, player.getCoins());
            statement.setBoolean(9, player.isVanished());
            statement.setBoolean(10, player.isCommandSpy());
            statement.execute();
        }
        catch (SQLException throwables)
        {
            throwables.printStackTrace();
        }
    }
}
