package dev.plex.api.storage;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Provides controlled access to Plex SQL storage.
 */
public interface StorageApi
{
    /**
     * Opens a SQL connection for the supplied callback and closes it afterwards.
     *
     * @param function callback that uses the connection
     * @param <T> callback result type
     * @return callback result
     * @throws SQLException if a connection or callback SQL operation fails
     */
    <T> T withConnection(SqlFunction<T> function) throws SQLException;

    /**
     * SQL callback used by {@link #withConnection(SqlFunction)}.
     *
     * @param <T> callback result type
     */
    @FunctionalInterface
    interface SqlFunction<T>
    {
        /**
         * Uses a SQL connection.
         *
         * @param connection open SQL connection
         * @return callback result
         * @throws SQLException if the callback cannot complete its SQL work
         */
        T apply(Connection connection) throws SQLException;
    }
}
