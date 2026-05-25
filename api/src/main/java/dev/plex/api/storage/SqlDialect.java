package dev.plex.api.storage;

/**
 * SQL dialects supported by Plex storage.
 */
public enum SqlDialect
{
    /**
     * SQLite storage.
     */
    SQLITE("sqlite"),
    /**
     * MariaDB or MySQL-compatible storage.
     */
    MARIADB("mariadb"),
    /**
     * PostgreSQL storage.
     */
    POSTGRES("postgres");

    private final String migrationDirectory;

    SqlDialect(String migrationDirectory)
    {
        this.migrationDirectory = migrationDirectory;
    }

    /**
     * Returns the resource directory name used for dialect-specific migrations.
     *
     * @return migration resource directory name
     */
    public String migrationDirectory()
    {
        return migrationDirectory;
    }
}
