package me.totalfreedom.plex.storage;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import me.totalfreedom.plex.Plex;
import me.totalfreedom.plex.PlexBase;

public class SQLConnection extends PlexBase
{
    private Connection connection;

    public Connection getCon()
    {
        String host = plugin.config.getString("data.central.hostname");
        int port = plugin.config.getInt("data.central.port");
        String username = plugin.config.getString("data.central.user");
        String password = plugin.config.getString("data.central.password");
        String database = plugin.config.getString("data.central.db");
        try
        {
            if (plugin.config.getString("data.central.storage").equalsIgnoreCase("sqlite"))
            {
                connection = DriverManager.getConnection("jdbc:sqlite:" + new File(plugin.getDataFolder(), "database.db").getAbsolutePath());
                plugin.setStorageType(StorageType.SQLITE);
            }
            else if (plugin.config.getString("data.central.storage").equalsIgnoreCase("mariadb"))
            {
                Class.forName("org.mariadb.jdbc.Driver");
                connection = DriverManager.getConnection("jdbc:mariadb://" + host + ":" + port + "/" + database, username, password);
                Plex.get().setStorageType(StorageType.SQL);
            }
        }
        catch (SQLException | ClassNotFoundException throwables)
        {
            throwables.printStackTrace();
        }

        try
        {
            if (connection != null)
            {
                connection.prepareStatement("CREATE TABLE IF NOT EXISTS `players` (\n" +
                        "\t`uuid` VARCHAR(46) NOT NULL,\n" +
                        "\t`name` VARCHAR(18),\n" +
                        "\t`login_msg` VARCHAR,\n" +
                        "\t`prefix` VARCHAR,\n" +
                        "\t`rank` VARCHAR,\n" +
                        "\t`ips` VARCHAR,\n" +
                        "\t`coins` BIGINT,\n" +
                        "\t`vanished` BOOLEAN,\n" +
                        "\tPRIMARY KEY (`uuid`)\n" +
                        ");").execute();
                connection.prepareStatement("CREATE TABLE IF NOT EXISTS `bans` (\n" +
                        "\t`banID` VARCHAR,\n" +
                        "\t`uuid` VARCHAR(46) NOT NULL,\n" +
                        "\t`banner` VARCHAR(46),\n" +
                        "\t`ip` VARCHAR,\n" +
                        "\t`reason` VARCHAR,\n" +
                        "\t`enddate` BIGINT,\n" +
                        "\t`active` BOOLEAN,\n" +
                        "\tPRIMARY KEY (`banID`)\n" +
                        ");").execute();
            }
        }
        catch (SQLException throwables)
        {
            throwables.printStackTrace();
        }
        return connection;
    }
}
