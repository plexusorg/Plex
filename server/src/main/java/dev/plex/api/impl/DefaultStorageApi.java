package dev.plex.api.impl;

import dev.plex.Plex;
import dev.plex.api.storage.StorageApi;
import java.sql.Connection;
import java.sql.SQLException;

final class DefaultStorageApi implements StorageApi
{
    private final Plex plugin;

    DefaultStorageApi(Plex plugin) { this.plugin = plugin; }

    @Override
    public <T> T withConnection(SqlFunction<T> function) throws SQLException
    {
        try (Connection connection = plugin.getSqlConnection().getCon())
        {
            return function.apply(connection);
        }
    }
}
