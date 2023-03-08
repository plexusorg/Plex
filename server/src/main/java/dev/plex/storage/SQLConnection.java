package dev.plex.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.plex.Plex;
import dev.plex.PlexBase;
import lombok.Getter;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

@Getter
public class SQLConnection implements PlexBase
{
    private HikariDataSource dataSource;

    public SQLConnection()
    {
        if (!plugin.config.getString("data.central.storage").equalsIgnoreCase("sqlite") && !plugin.config.getString("data.central.storage").equalsIgnoreCase("mariadb"))
        {
            return;
        }

        String host = plugin.config.getString("data.central.hostname");
        int port = plugin.config.getInt("data.central.port");
        String username = plugin.config.getString("data.central.user");
        String password = plugin.config.getString("data.central.password");
        String database = plugin.config.getString("data.central.db");

        HikariConfig config = new HikariConfig();
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        this.dataSource = new HikariDataSource();
        dataSource.setMaxLifetime(15000);
        dataSource.setIdleTimeout(15000 * 2);
        dataSource.setConnectionTimeout(15000 * 4);
        dataSource.setMinimumIdle(2);
        dataSource.setMaximumPoolSize(10);
        try
        {
            if (plugin.config.getString("data.central.storage").equalsIgnoreCase("sqlite"))
            {
                dataSource.setJdbcUrl("jdbc:sqlite:" + new File(plugin.getDataFolder(), "database.db").getAbsolutePath());
                plugin.setStorageType(StorageType.SQLITE);
            }
            else if (plugin.config.getString("data.central.storage").equalsIgnoreCase("mariadb"))
            {
                Class.forName("org.mariadb.jdbc.Driver");
                dataSource.setJdbcUrl("jdbc:mariadb://" + host + ":" + port + "/" + database);
                dataSource.setUsername(username);
                dataSource.setPassword(password);
                Plex.get().setStorageType(StorageType.MARIADB);
            }
        }
        catch (ClassNotFoundException throwables)
        {
            throwables.printStackTrace();
        }

        try (Connection con = getCon())
        {
            con.prepareStatement("CREATE TABLE IF NOT EXISTS `players` (" +
                    "`uuid` VARCHAR(46) NOT NULL, " +
                    "`name` VARCHAR(18), " +
                    "`login_msg` VARCHAR(2000), " +
                    "`prefix` VARCHAR(2000), " +
                    "`rank` VARCHAR(20), " +
                    "`adminActive` BOOLEAN, " +
                    "`staffChat` BOOLEAN, " +
                    "`ips` VARCHAR(2000), " +
                    "`coins` BIGINT, " +
                    "`vanished` BOOLEAN, " +
                    "`commandspy` BOOLEAN, " +
                    "PRIMARY KEY (`uuid`));").execute();
            con.prepareStatement("CREATE TABLE IF NOT EXISTS `punishments` (" +
                    "`punished` VARCHAR(46) NOT NULL, " +
                    "`punisher` VARCHAR(46), " +
                    "`punishedUsername` VARCHAR(16), " +
                    "`ip` VARCHAR(2000), " +
                    "`type` VARCHAR(30), " +
                    "`reason` VARCHAR(2000), " +
                    "`customTime` BOOLEAN, " +
                    "`active` BOOLEAN, " +
                    "`endDate` BIGINT" +
                    ");").execute();
            con.prepareStatement("CREATE TABLE IF NOT EXISTS `notes` (" +
                    "`id` INT NOT NULL, " +
                    "`uuid` VARCHAR(46) NOT NULL, " +
                    "`written_by` VARCHAR(46), " +
                    "`note` VARCHAR(2000), " +
                    "`timestamp` BIGINT" +
                    ");").execute();
            con.prepareStatement("CREATE TABLE IF NOT EXISTS `permissions` (" +
                    "`uuid` VARCHAR(46) NOT NULL," +
                    "`permission` VARCHAR(1000) NOT NULL," +
                    "`allowed` BOOLEAN" +
                    ");").execute();
            // Plex 1.2

            try
            {
                con.prepareStatement("ALTER TABLE `players` ADD COLUMN `staffChat` BOOLEAN DEFAULT false;").execute();
            }
            catch (SQLException ignored)
            {
            }
        }
        catch (SQLException throwables)
        {
            throwables.printStackTrace();
        }
    }

    public Connection getCon()
    {
        if (this.dataSource == null)
        {
            return null;
        }
        try
        {
            return dataSource.getConnection();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return null;
    }
}
