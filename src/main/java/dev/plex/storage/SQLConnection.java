package dev.plex.storage;

import dev.plex.Plex;
import dev.plex.PlexBase;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

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
                Plex.get().setStorageType(StorageType.MARIADB);
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
                connection.prepareStatement("CREATE TABLE IF NOT EXISTS `players` (" +
                        "`uuid` VARCHAR(46) NOT NULL, " +
                        "`name` VARCHAR(18), " +
                        "`login_msg` VARCHAR(70), " +
                        "`prefix` VARCHAR(45), " +
                        "`rank` VARCHAR(20), " +
                        "`ips` VARCHAR(2000), " +
                        "`coins` BIGINT, " +
                        "`vanished` BOOLEAN, " +
                        "`commandSpy` BOOLEAN, " +
                        "PRIMARY KEY (`uuid`));").execute();
                connection.prepareStatement("CREATE TABLE IF NOT EXISTS `bans` (" +
                        "`banID` VARCHAR(46), " +
                        "`uuid` VARCHAR(46) NOT NULL, " +
                        "`banner` VARCHAR(46), " +
                        "`ip` VARCHAR(2000), " +
                        "`reason` VARCHAR(256), " +
                        "`enddate` BIGINT, " +
                        "`active` BOOLEAN, " +
                        "PRIMARY KEY (`banID`)" +
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
