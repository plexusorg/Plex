package dev.plex.api.storage;

import java.sql.SQLException;

/**
 * Callback used for SQL work that does not receive a connection directly.
 *
 * @param <T> callback result type
 */
@FunctionalInterface
public interface SqlCallable<T>
{
    /**
     * Runs SQL work.
     *
     * @return callback result
     * @throws SQLException if the SQL work cannot complete
     */
    T call() throws SQLException;
}
