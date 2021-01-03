package dev.plex.cache;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import dev.plex.Plex;
import dev.plex.player.PlexPlayer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

public class SQLPlayerData
{
    private final String SELECT = "SELECT * FROM `players` WHERE uuid=?";
    private final String UPDATE = "UPDATE `players` SET name=?, login_msg=?, prefix=?, rank=?, ips=?, coins=?, vanished=? WHERE uuid=?";
    private final String INSERT = "INSERT INTO `players` (`uuid`, `name`, `login_msg`, `prefix`, `rank`, `ips`, `coins`, `vanished`) VALUES (?, ?, ?, ?, ?, ?, ?, ?);";

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

    public PlexPlayer getByUUID(UUID uuid)
    {
        if (PlayerCache.getPlexPlayerMap().containsKey(uuid))
        {
            return PlayerCache.getPlexPlayerMap().get(uuid);
        }

        try (Connection con = Plex.get().getSqlConnection().getCon())
        {
            PreparedStatement statement = con.prepareStatement(SELECT);
            statement.setString(1, uuid.toString());
            ResultSet set = statement.executeQuery();
            PlexPlayer plexPlayer = new PlexPlayer(uuid);
            while (set.next())
            {
                String name = set.getString("name");
                String loginMSG = set.getString("login_msg");
                String prefix = set.getString("prefix");
                String rankName = set.getString("rank").toUpperCase();
                long coins = set.getLong("coins");
                boolean vanished = set.getBoolean("vanished");
                List<String> ips = new Gson().fromJson(set.getString("ips"), new TypeToken<List<String>>(){}.getType());
                plexPlayer.setName(name);
                plexPlayer.setLoginMSG(loginMSG);
                plexPlayer.setPrefix(prefix);
                plexPlayer.setRank(rankName);
                plexPlayer.setIps(ips);
                plexPlayer.setCoins(coins);
                plexPlayer.setVanished(vanished);
            }
            return plexPlayer;
        }
        catch (SQLException throwables)
        {
            throwables.printStackTrace();
        }
        return null;
    }

    public void update(PlexPlayer player)
    {
        try (Connection con = Plex.get().getSqlConnection().getCon())
        {
            PreparedStatement statement = con.prepareStatement(UPDATE);
            statement.setString(1, player.getName());
            statement.setString(2, player.getLoginMSG());
            statement.setString(3, player.getPrefix());
            statement.setString(4, player.getRank().toLowerCase());
            statement.setString(5, new Gson().toJson(player.getIps()));
            statement.setLong(6, player.getCoins());
            statement.setBoolean(7, player.isVanished());
            statement.setString(8, player.getUuid());
            statement.executeUpdate();
        }
        catch (SQLException throwables)
        {
            throwables.printStackTrace();
        }
    }

    public void insert(PlexPlayer player)
    {
        try (Connection con = Plex.get().getSqlConnection().getCon())
        {
            PreparedStatement statement = con.prepareStatement(INSERT);
            statement.setString(1, player.getUuid());
            statement.setString(2, player.getName());
            statement.setString(3, player.getLoginMSG());
            statement.setString(4, player.getPrefix());
            statement.setString(5, player.getRank().toLowerCase());
            statement.setString(6, new Gson().toJson(player.getIps()));
            statement.setLong(7, player.getCoins());
            statement.setBoolean(8, player.isVanished());
            statement.execute();
        }
        catch (SQLException throwables)
        {
            throwables.printStackTrace();
        }
    }

}
