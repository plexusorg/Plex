package dev.plex.api.storage;

import dev.plex.module.PlexModule;

/**
 * Provides SQL storage for modules.
 */
public interface StorageApi
{
    /**
     * Returns storage for a module.
     *
     * @param module module requesting storage
     * @return module storage
     */
    ModuleStorage forModule(PlexModule module);

    /**
     * Returns the configured SQL dialect.
     *
     * @return configured SQL dialect
     */
    SqlDialect dialect();

}
