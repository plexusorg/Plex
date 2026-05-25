package dev.plex.storage.module;

import com.j256.ormlite.misc.TransactionManager;
import dev.plex.Plex;
import dev.plex.api.storage.ModuleMigrations;
import dev.plex.api.storage.ModuleOrm;
import dev.plex.api.storage.ModuleStorage;
import dev.plex.api.storage.SqlCallable;
import dev.plex.module.PlexModule;

import java.sql.SQLException;

public class ServerModuleStorage implements ModuleStorage
{
    private final Plex plugin;
    private final PlexModule module;
    private final String prefix;
    private final ModuleMigrations migrations;
    private final ModuleOrm orm;

    public ServerModuleStorage(Plex plugin, PlexModule module)
    {
        this.plugin = plugin;
        this.module = module;
        this.prefix = ModuleNames.prefix(module);
        this.migrations = new ServerModuleMigrations(plugin, module, this);
        this.orm = new ServerModuleOrm(plugin.getSqlConnection().getConnectionSource(), this);
    }

    @Override
    public String prefix()
    {
        return prefix;
    }

    @Override
    public String table(String localName)
    {
        return ModuleNames.table(prefix, localName);
    }

    String quotedTable(String localName)
    {
        return plugin.getStorageType().quoteIdentifier(table(localName));
    }

    String scope()
    {
        return "module:" + prefix;
    }

    @Override
    public ModuleMigrations migrations()
    {
        return migrations;
    }

    @Override
    public ModuleOrm orm()
    {
        return orm;
    }

    @Override
    public <T> T transaction(SqlCallable<T> callable) throws SQLException
    {
        return TransactionManager.callInTransaction(plugin.getSqlConnection().getConnectionSource(), callable::call);
    }
}
