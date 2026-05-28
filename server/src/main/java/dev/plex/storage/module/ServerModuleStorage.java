package dev.plex.storage.module;

import dev.plex.Plex;
import dev.plex.api.storage.ModuleMigrations;
import dev.plex.api.storage.ModuleStorage;
import dev.plex.module.PlexModule;
import org.jdbi.v3.core.Jdbi;

public class ServerModuleStorage implements ModuleStorage
{
    private final Plex plugin;
    private final PlexModule module;
    private final String prefix;
    private final ModuleMigrations migrations;

    public ServerModuleStorage(Plex plugin, PlexModule module)
    {
        this.plugin = plugin;
        this.module = module;
        this.prefix = ModuleNames.prefix(module);
        this.migrations = new ServerModuleMigrations(plugin, module, this);
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
    public Jdbi jdbi()
    {
        return plugin.getDatabase().getJdbi();
    }
}
