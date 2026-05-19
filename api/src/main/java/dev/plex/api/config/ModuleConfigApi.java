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
     * @param from resource path to copy defaults from
     * @param to destination file path relative to the module data folder
     * @return module configuration wrapper
     */
    ModuleConfiguration create(PlexModule module, String from, String to);
}
