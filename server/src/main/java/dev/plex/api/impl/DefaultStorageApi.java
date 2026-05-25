package dev.plex.api.impl;

import dev.plex.Plex;
import dev.plex.api.storage.ModuleStorage;
import dev.plex.api.storage.SqlDialect;
import dev.plex.api.storage.StorageApi;
import dev.plex.module.PlexModule;
import dev.plex.storage.module.ServerModuleStorage;
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

    @Override
    public ModuleStorage forModule(PlexModule module)
    {
        return new ServerModuleStorage(plugin, module);
    }

    @Override
    public SqlDialect dialect()
    {
        return plugin.getStorageType().dialect();
    }
}
