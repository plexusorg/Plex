package dev.plex.api.storage;

import org.jdbi.v3.core.Jdbi;

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
     * Returns the shared JDBI instance. Build SQL with {@link #table(String)} for
     * physical-table resolution; use {@code jdbi().inTransaction(...)} for multi-statement transactions.
     *
     * @return shared JDBI instance
     */
    Jdbi jdbi();
}
