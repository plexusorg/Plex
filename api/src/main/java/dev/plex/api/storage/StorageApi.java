package dev.plex.api.storage;

import java.sql.Connection;
import java.sql.SQLException;

public interface StorageApi
{
    <T> T withConnection(SqlFunction<T> function) throws SQLException;

    @FunctionalInterface
    interface SqlFunction<T>
    {
        T apply(Connection connection) throws SQLException;
    }
}
