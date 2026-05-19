package dev.plex.storage.database;

import com.j256.ormlite.jdbc.DataSourceConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import dev.plex.Plex;
import dev.plex.storage.StorageType;
import dev.plex.util.PlexLog;
import lombok.Getter;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Getter
public class Database
{
    private static final String MIGRATION_TABLE = "plex_schema_history";

    protected final Plex plugin;
    private final HikariDataSource dataSource;
    private final ConnectionSource connectionSource;
    private final StorageType storageType;

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
            this.connectionSource = new DataSourceConnectionSource(dataSource, config.getJdbcUrl());
            runMigrations();
        }
        catch (Exception e)
        {
            dataSource.close();
            throw new IllegalStateException("Failed to initialize database", e);
        }
    }

    private void runMigrations() throws Exception
    {
        try (Connection connection = dataSource.getConnection())
        {
            ensureMigrationTable(connection);
            for (String migration : List.of("001_initial_schema"))
            {
                if (hasMigration(connection, migration))
                {
                    continue;
                }

                executeMigration(connection, migration);
                try (Statement statement = connection.createStatement())
                {
                    statement.executeUpdate("INSERT INTO " + MIGRATION_TABLE + " (version) VALUES ('" + migration + "')");
                }
                PlexLog.log("Applied database migration " + migration);
            }
        }
    }

    private void ensureMigrationTable(Connection connection) throws SQLException
    {
        try (Statement statement = connection.createStatement())
        {
            statement.execute(storageType.migrationHistoryTableSql(MIGRATION_TABLE));
        }
    }

    private boolean hasMigration(Connection connection, String migration) throws SQLException
    {
        try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery("SELECT version FROM " + MIGRATION_TABLE + " WHERE version = '" + migration + "'"))
        {
            return resultSet.next();
        }
    }

    private void executeMigration(Connection connection, String migration) throws Exception
    {
        String resource = "db/migration/" + storageType.getMigrationDirectory() + "/" + migration + ".sql";
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(resource))
        {
            if (stream == null)
            {
                throw new IllegalStateException("Missing database migration resource: " + resource);
            }

            for (String sql : splitStatements(new String(stream.readAllBytes(), StandardCharsets.UTF_8)))
            {
                try (Statement statement = connection.createStatement())
                {
                    statement.execute(sql);
                }
            }
        }
    }

    private List<String> splitStatements(String script)
    {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;

        for (int i = 0; i < script.length(); i++)
        {
            char c = script.charAt(i);
            if (c == '\'' && !inDoubleQuote)
            {
                inSingleQuote = !inSingleQuote;
            }
            else if (c == '"' && !inSingleQuote)
            {
                inDoubleQuote = !inDoubleQuote;
            }

            if (c == ';' && !inSingleQuote && !inDoubleQuote)
            {
                addStatement(statements, current);
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        addStatement(statements, current);
        return statements;
    }

    private void addStatement(List<String> statements, StringBuilder statement)
    {
        String sql = statement.toString().replaceAll("(?m)^\\s*--.*$", "").trim();
        if (!sql.isEmpty())
        {
            statements.add(sql);
        }
    }

    public Connection getConnection() throws SQLException
    {
        return dataSource.getConnection();
    }

    public void close()
    {
        try
        {
            connectionSource.close();
        }
        catch (Exception e)
        {
            PlexLog.warn("Failed to close ORMLite connection source: " + e.getMessage());
        }
        dataSource.close();
    }
}
