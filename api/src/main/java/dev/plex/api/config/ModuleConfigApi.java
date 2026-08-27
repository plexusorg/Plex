package dev.plex.api.config;

import dev.plex.module.PlexModule;

/**
 * Creates configuration files owned by Plex modules.
 */
public interface ModuleConfigApi
{
    /**
     * Creates or opens a module configuration.
     *
     * @param module module that owns the configuration
     * @param fileName resource path and module data file path
     * @return module configuration
     */
    ModuleConfiguration create(PlexModule module, String fileName);
}
