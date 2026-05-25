package dev.plex.storage.database;

import dev.plex.module.PlexModule;
import dev.plex.storage.StorageType;
import dev.plex.util.PlexLog;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MigrationRunner
{
    private static final String MIGRATION_TABLE = "plex_schema_history";
    private static final Pattern VERSION_PATTERN = Pattern.compile("^[0-9]{3}_[a-z0-9_]+$");
    private static final Pattern TABLE_TOKEN_PATTERN = Pattern.compile("\\{\\{table:([a-z0-9_]+)}}");

    private final StorageType storageType;

    public MigrationRunner(StorageType storageType)
    {
        this.storageType = storageType;
    }

    public void runCore(DataSource dataSource, ClassLoader classLoader, List<String> versions) throws SQLException
    {
        run(dataSource, "core", versions, version -> readCore(classLoader, version), Function.identity());
    }

    public void runModule(DataSource dataSource, PlexModule module, String scope, String resourceRoot, List<String> versions, Function<String, String> tableResolver) throws SQLException
    {
        run(dataSource, scope, versions, version -> readModule(module, resourceRoot, version), tableResolver);
    }

    private void run(DataSource dataSource, String scope, List<String> versions, ResourceReader reader, Function<String, String> tableResolver) throws SQLException
    {
        try (Connection connection = dataSource.getConnection())
        {
            ensureMigrationTable(connection);
            for (String version : versions)
            {
                validateVersion(version);
                if (hasMigration(connection, scope, version))
                {
                    continue;
                }

                String script = replaceTableTokens(reader.read(version), tableResolver);
                for (String sql : splitStatements(script))
                {
                    try (Statement statement = connection.createStatement())
                    {
                        statement.execute(sql);
                    }
                }
                insertMigration(connection, scope, version);
                PlexLog.log("Applied database migration " + scope + ":" + version);
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

    private boolean hasMigration(Connection connection, String scope, String version) throws SQLException
    {
        try (PreparedStatement statement = connection.prepareStatement("SELECT version FROM " + MIGRATION_TABLE + " WHERE scope = ? AND version = ?"))
        {
            statement.setString(1, scope);
            statement.setString(2, version);
            try (ResultSet resultSet = statement.executeQuery())
            {
                return resultSet.next();
            }
        }
    }

    private void insertMigration(Connection connection, String scope, String version) throws SQLException
    {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO " + MIGRATION_TABLE + " (scope, version) VALUES (?, ?)"))
        {
            statement.setString(1, scope);
            statement.setString(2, version);
            statement.executeUpdate();
        }
    }

    private void validateVersion(String version) throws SQLException
    {
        if (!VERSION_PATTERN.matcher(version).matches())
        {
            throw new SQLException("Invalid migration version: " + version);
        }
    }

    private String readCore(ClassLoader classLoader, String version) throws SQLException
    {
        String resource = "db/migration/" + storageType.dialect().migrationDirectory() + "/" + version + ".sql";
        try (InputStream stream = classLoader.getResourceAsStream(resource))
        {
            if (stream == null)
            {
                throw new SQLException("Missing database migration resource: " + resource);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            throw new SQLException("Failed to read database migration resource: " + resource, e);
        }
    }

    private String readModule(PlexModule module, String resourceRoot, String version) throws SQLException
    {
        String resource = resourceRoot + "/" + storageType.dialect().migrationDirectory() + "/" + version + ".sql";
        try (InputStream stream = module.getResource(resource))
        {
            if (stream == null)
            {
                throw new SQLException("Missing module migration resource: " + resource);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            throw new SQLException("Failed to read module migration resource: " + resource, e);
        }
    }

    private String replaceTableTokens(String script, Function<String, String> tableResolver) throws SQLException
    {
        Matcher matcher = TABLE_TOKEN_PATTERN.matcher(script);
        StringBuilder replaced = new StringBuilder();
        while (matcher.find())
        {
            matcher.appendReplacement(replaced, Matcher.quoteReplacement(tableResolver.apply(matcher.group(1))));
        }
        matcher.appendTail(replaced);
        if (replaced.toString().contains("{{table:"))
        {
            throw new SQLException("Unsupported table token in migration");
        }
        return replaced.toString();
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

    @FunctionalInterface
    private interface ResourceReader
    {
        String read(String version) throws SQLException;
    }
}
