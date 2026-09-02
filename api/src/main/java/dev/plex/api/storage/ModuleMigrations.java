package dev.plex.api.storage;

import java.sql.SQLException;

/**
 * Runs database migrations for a module.
 *
 * <p>Migration scripts are discovered from the module's
 * {@code db/migration/<dialect>} directory. A version must use the form
 * {@code 001_short_name}. Scripts can use {@code {{table:local_name}}} for
 * module table names.</p>
 */
public interface ModuleMigrations
{
    /**
     * Runs this module's migrations.
     *
     * @throws SQLException if a migration cannot be applied
     */
    void run() throws SQLException;
}
