package dev.plex.storage.module;

import dev.plex.Plex;
import dev.plex.api.storage.ModuleMigrations;
import dev.plex.module.PlexModule;

import java.sql.SQLException;

public class ServerModuleMigrations implements ModuleMigrations
{
    private final Plex plugin;
    private final PlexModule module;
    private final ServerModuleStorage storage;

    public ServerModuleMigrations(Plex plugin, PlexModule module, ServerModuleStorage storage)
    {
        this.plugin = plugin;
        this.module = module;
        this.storage = storage;
    }

    @Override
    public void run() throws SQLException
    {
        plugin.getDatabase().getMigrationRunner().runModule(
                plugin.getDatabase().getDataSource(),
                module,
                storage.scope(),
                storage::quotedTable
        );
    }
}
