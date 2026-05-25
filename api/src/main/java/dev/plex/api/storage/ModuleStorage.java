package dev.plex.api.storage;

import java.sql.SQLException;

/**
 * Module-scoped storage namespace.
 */
public interface ModuleStorage
{
    /**
     * Returns the validated module table prefix.
     *
     * @return module table prefix
     */
    String prefix();

    /**
     * Resolves a local table name to the module's physical table name.
     *
     * @param localName module-local table name
     * @return physical table name
     */
    String table(String localName);

    /**
     * Returns module migration operations.
     *
     * @return module migration operations
     */
    ModuleMigrations migrations();

    /**
     * Returns module ORMLite DAO operations.
     *
     * @return module ORMLite DAO operations
     */
    ModuleOrm orm();

    /**
     * Runs work inside a storage transaction.
     *
     * @param callable work to run
     * @param <T> callback result type
     * @return callback result
     * @throws SQLException if the transaction cannot complete
     */
    <T> T transaction(SqlCallable<T> callable) throws SQLException;
}
