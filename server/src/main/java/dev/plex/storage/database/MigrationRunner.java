package dev.plex.storage.database;

import dev.plex.module.PlexModule;
import dev.plex.storage.StorageType;
import dev.plex.util.PlexLog;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MigrationRunner
{
    private static final String MIGRATION_TABLE = "plex_schema_history";
    private static final String MIGRATION_ROOT = "db/migration";
    private static final Pattern VERSION_PATTERN = Pattern.compile("^[0-9]{3}_[a-z0-9_]+$");
    private static final Pattern TABLE_TOKEN_PATTERN = Pattern.compile("\\{\\{table:([a-z0-9_]+)}}");

    private final StorageType storageType;

    public MigrationRunner(StorageType storageType)
    {
        this.storageType = storageType;
    }

    public void runCore(DataSource dataSource) throws SQLException
    {
        ClassLoader classLoader = MigrationRunner.class.getClassLoader();
        String resourceDirectory = migrationDirectory();
        run(dataSource, "core", discoverCore(classLoader, resourceDirectory),
                version -> readCore(classLoader, version), Function.identity());
    }

    public void runModule(DataSource dataSource, PlexModule module, File moduleJar, String scope,
                          Function<String, String> tableResolver) throws SQLException
    {
        String resourceDirectory = migrationDirectory();
        run(dataSource, scope, discoverModule(module, moduleJar, resourceDirectory),
                version -> readModule(module, version), tableResolver);
    }

    private void run(DataSource dataSource, String scope, List<String> versions, ResourceReader reader, Function<String, String> tableResolver) throws SQLException
    {
        try (Connection connection = dataSource.getConnection())
        {
            ensureMigrationTable(connection);
            for (String version : versions)
            {
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
        String resource = migrationDirectory() + "/" + version + ".sql";
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

    private String readModule(PlexModule module, String version) throws SQLException
    {
        String resource = migrationDirectory() + "/" + version + ".sql";
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

    private String migrationDirectory()
    {
        return MIGRATION_ROOT + "/" + storageType.dialect().migrationDirectory();
    }

    private List<String> discoverCore(ClassLoader classLoader, String resourceDirectory) throws SQLException
    {
        TreeSet<String> versions = new TreeSet<>();
        try
        {
            Enumeration<URL> resources = classLoader.getResources(resourceDirectory);
            while (resources.hasMoreElements())
            {
                URL resource = resources.nextElement();
                if (resource.getProtocol().equals("file"))
                {
                    try (var paths = Files.list(Path.of(resource.toURI())))
                    {
                        paths.filter(Files::isRegularFile)
                                .map(path -> path.getFileName().toString())
                                .forEach(file -> addMigration(versions, file));
                    }
                }
                else if (resource.getProtocol().equals("jar"))
                {
                    JarURLConnection connection = (JarURLConnection)resource.openConnection();
                    connection.setUseCaches(false);
                    try (JarFile jar = connection.getJarFile())
                    {
                        addMigrations(versions, jar, resourceDirectory);
                    }
                }
            }
        }
        catch (IOException | URISyntaxException e)
        {
            throw new SQLException("Failed to discover database migrations", e);
        }
        return requireMigrations(versions, resourceDirectory);
    }

    private List<String> discoverModule(PlexModule module, File moduleJar, String resourceDirectory) throws SQLException
    {
        TreeSet<String> versions = new TreeSet<>();
        try (JarFile jar = new JarFile(moduleJar))
        {
            addMigrations(versions, jar, resourceDirectory);
        }
        catch (IOException e)
        {
            throw new SQLException("Failed to discover migrations for module " + module.getPlexModuleFile().getName(), e);
        }
        return requireMigrations(versions, resourceDirectory);
    }

    private void addMigrations(TreeSet<String> versions, JarFile jar, String resourceDirectory)
    {
        String prefix = resourceDirectory + "/";
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements())
        {
            JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (!entry.isDirectory() && name.startsWith(prefix) && !name.substring(prefix.length()).contains("/"))
            {
                addMigration(versions, name.substring(prefix.length()));
            }
        }
    }

    private void addMigration(TreeSet<String> versions, String fileName)
    {
        if (fileName.endsWith(".sql"))
        {
            versions.add(fileName.substring(0, fileName.length() - 4));
        }
    }

    private List<String> requireMigrations(TreeSet<String> versions, String resourceDirectory) throws SQLException
    {
        if (versions.isEmpty())
        {
            throw new SQLException("No database migrations found in " + resourceDirectory);
        }
        for (String version : versions)
        {
            validateVersion(version);
        }
        return List.copyOf(versions);
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
