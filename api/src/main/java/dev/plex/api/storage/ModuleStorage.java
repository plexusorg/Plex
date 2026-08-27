package dev.plex.api.storage;

import org.jdbi.v3.core.Jdbi;

/**
 * Provides SQL storage for one module.
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
     * Adds the module prefix to a local table name.
     *
    * @param localName module-local table name
     * @return full table name
     * @throws IllegalArgumentException if the name is not a lowercase SQL name
     */
    String table(String localName);

    /**
     * Returns module migration operations.
     *
     * @return module migration operations
     */
    ModuleMigrations migrations();

    /**
     * Returns the shared Jdbi instance.
     * Use {@link #table(String)} for each module table. Use
     * {@code jdbi().inTransaction(...)} for a transaction with multiple statements.
     *
     * @return shared JDBI instance
     */
    Jdbi jdbi();
}
