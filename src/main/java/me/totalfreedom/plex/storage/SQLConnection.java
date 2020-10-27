package me.totalfreedom.plex.storage;

import me.totalfreedom.plex.Plex;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class SQLConnection
{
    private Plex plugin = Plex.get();

    private Connection connection;

    public Connection getCon()
    {
        String host = plugin.getConfig().getString("data.central.hostname");
        int port = plugin.getConfig().getInt("data.central.port");
        String username = plugin.getConfig().getString("data.central.user");
        String password = plugin.getConfig().getString("data.central.password");
        String database = plugin.getConfig().getString("data.central.database");

        try {
            if (plugin.getConfig().getString("data.central.storage").equalsIgnoreCase("sqlite"))
            {
                connection = DriverManager.getConnection("jdbc:sqlite:" + new File(plugin.getDataFolder(), "database.db").getAbsolutePath());
                Plex.get().setStorageType(StorageType.SQLITE);
            }
            else if (plugin.getConfig().getString("data.central.storage").equalsIgnoreCase("mysql"))
            {
                connection = DriverManager.getConnection("jdbc:mysql://" + host + ":" + port + "/" + database, username, password);
                Plex.get().setStorageType(StorageType.MONGO);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return connection;
    }


}
