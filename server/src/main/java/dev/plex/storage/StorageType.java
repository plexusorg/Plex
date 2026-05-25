package dev.plex.storage;

import com.zaxxer.hikari.HikariConfig;
import dev.plex.Plex;
import dev.plex.api.storage.SqlDialect;

import java.io.File;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

public enum StorageType
{
    SQLITE("SQLite", "sqlite", "org.sqlite.JDBC", Set.of("sqlite"))
            {
                @Override
                public void configure(HikariConfig config, Plex plugin)
                {
                    File databaseFile = new File(plugin.getDataFolder(), "database.db");
                    config.setJdbcUrl("jdbc:sqlite:" + databaseFile.getAbsolutePath());
                    config.setDriverClassName(driverClass);
                    config.setMaximumPoolSize(1);
                }

                @Override
                public String migrationHistoryTableSql(String tableName)
                {
                    return "CREATE TABLE IF NOT EXISTS " + quoteIdentifier(tableName) + " (scope VARCHAR(100) NOT NULL, version VARCHAR(100) NOT NULL, installed_at INTEGER NOT NULL DEFAULT (strftime('%s','now') * 1000), PRIMARY KEY (scope, version))";
                }
            },

    MARIADB("MariaDB", "mariadb", "org.mariadb.jdbc.Driver", Set.of("mariadb", "mysql"))
            {
                @Override
                public void configure(HikariConfig config, Plex plugin)
                {
                    configureRemote(config, plugin, "jdbc:mariadb://", driverClass);
                    config.addDataSourceProperty("cachePrepStmts", "true");
                    config.addDataSourceProperty("prepStmtCacheSize", "250");
                    config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                }

                @Override
                public String migrationHistoryTableSql(String tableName)
                {
                    return "CREATE TABLE IF NOT EXISTS " + quoteIdentifier(tableName) + " (`scope` VARCHAR(100) NOT NULL, `version` VARCHAR(100) NOT NULL, `installed_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (`scope`, `version`))";
                }
            },

    POSTGRES("PostgreSQL", "postgres", "org.postgresql.Driver", Set.of("postgres", "postgresql"))
            {
                @Override
                public void configure(HikariConfig config, Plex plugin)
                {
                    configureRemote(config, plugin, "jdbc:postgresql://", driverClass);
                    config.addDataSourceProperty("cachePrepStmts", "true");
                    config.addDataSourceProperty("prepStmtCacheSize", "250");
                }
            };

    private final Set<String> aliases;
    private final String displayName;
    private final String migrationDirectory;
    protected final String driverClass;

    StorageType(String displayName, String migrationDirectory, String driverClass, Set<String> aliases)
    {
        this.aliases = aliases;
        this.displayName = displayName;
        this.migrationDirectory = migrationDirectory;
        this.driverClass = driverClass;
    }

    public static StorageType fromConfig(String value)
    {
        String normalized = value == null ? "sqlite" : value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(type -> type.aliases.contains(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported database storage type: " + value));
    }

    public abstract void configure(HikariConfig config, Plex plugin);

    public SqlDialect dialect()
    {
        return switch (this)
        {
            case SQLITE -> SqlDialect.SQLITE;
            case MARIADB -> SqlDialect.MARIADB;
            case POSTGRES -> SqlDialect.POSTGRES;
        };
    }

    public String quoteIdentifier(String identifier)
    {
        return switch (this)
        {
            case MARIADB -> "`" + identifier + "`";
            case SQLITE, POSTGRES -> "\"" + identifier + "\"";
        };
    }

    public String migrationHistoryTableSql(String tableName)
    {
        return "CREATE TABLE IF NOT EXISTS " + quoteIdentifier(tableName) + " (scope VARCHAR(100) NOT NULL, version VARCHAR(100) NOT NULL, installed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP, PRIMARY KEY (scope, version))";
    }

    public String playerModuleDataUpsertSql()
    {
        return switch (this)
        {
            case SQLITE -> """
                    INSERT INTO player_module_data (player_uuid, module, data_key, value_json, updated_at)
                    VALUES (?, ?, ?, ?, ?)
                    ON CONFLICT(player_uuid, module, data_key) DO UPDATE SET
                        value_json = excluded.value_json,
                        updated_at = excluded.updated_at
                    """;
            case MARIADB -> """
                    INSERT INTO `player_module_data` (`player_uuid`, `module`, `data_key`, `value_json`, `updated_at`)
                    VALUES (?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        `value_json` = VALUES(`value_json`),
                        `updated_at` = VALUES(`updated_at`)
                    """;
            case POSTGRES -> """
                    INSERT INTO player_module_data (player_uuid, module, data_key, value_json, updated_at)
                    VALUES (?, ?, ?, ?, ?)
                    ON CONFLICT(player_uuid, module, data_key) DO UPDATE SET
                        value_json = excluded.value_json,
                        updated_at = excluded.updated_at
                    """;
        };
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public String getMigrationDirectory()
    {
        return migrationDirectory;
    }

    private static void configureRemote(HikariConfig config, Plex plugin, String jdbcPrefix, String driverClass)
    {
        String host = plugin.config.getString("data.db.hostname");
        int port = plugin.config.getInt("data.db.port");
        String database = plugin.config.getString("data.db.name");
        config.setJdbcUrl(jdbcPrefix + host + ":" + port + "/" + database);
        config.setDriverClassName(driverClass);
        config.setUsername(plugin.config.getString("data.db.user"));
        config.setPassword(plugin.config.getString("data.db.password"));
    }
}
