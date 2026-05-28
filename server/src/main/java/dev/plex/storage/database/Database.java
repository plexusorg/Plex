package dev.plex.storage.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.plex.Plex;
import dev.plex.storage.StorageType;
import lombok.Getter;
import org.jdbi.v3.core.Jdbi;

import java.util.List;

@Getter
public class Database
{
    protected final Plex plugin;
    private final HikariDataSource dataSource;
    private final Jdbi jdbi;
    private final StorageType storageType;
    private final MigrationRunner migrationRunner;

    public Database(Plex plugin)
    {
        this.plugin = plugin;
        this.storageType = StorageType.fromConfig(plugin.config.getString("data.db.storage", "sqlite"));

        HikariConfig config = new HikariConfig();
        config.setPoolName("Plex-Database");
        config.setMaxLifetime(1_800_000);
        config.setIdleTimeout(600_000);
        config.setKeepaliveTime(0);
        config.setConnectionTimeout(60_000);
        config.setMinimumIdle(2);
        config.setMaximumPoolSize(10);
        storageType.configure(config, plugin);

        plugin.setStorageType(this.storageType);
        this.dataSource = new HikariDataSource(config);
        try
        {
            this.migrationRunner = new MigrationRunner(storageType);
            this.migrationRunner.runCore(dataSource, getClass().getClassLoader(), List.of("001_initial_schema"));
            this.jdbi = Jdbi.create(dataSource);
        }
        catch (Exception e)
        {
            dataSource.close();
            throw new IllegalStateException("Failed to initialize database", e);
        }
    }

    public java.sql.Connection getConnection() throws java.sql.SQLException
    {
        return dataSource.getConnection();
    }

    public void close()
    {
        dataSource.close();
    }
}
