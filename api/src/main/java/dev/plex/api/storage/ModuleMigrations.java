package dev.plex.api.storage;

import java.sql.SQLException;
import java.util.List;

/**
 * Runs database migrations for a module storage namespace.
 */
public interface ModuleMigrations
{
    /**
     * Runs migrations from the default module migration resource root.
     *
     * @param versions migration versions to apply
     * @throws SQLException if a migration cannot be applied
     */
    void run(List<String> versions) throws SQLException;

    /**
     * Runs migrations from a custom module migration resource root.
     *
     * @param resourceRoot resource root containing dialect migration folders
     * @param versions migration versions to apply
     * @throws SQLException if a migration cannot be applied
     */
    void run(String resourceRoot, List<String> versions) throws SQLException;
}
