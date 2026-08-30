package dev.plex.api.config;

import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Mutable YAML configuration owned by a Plex module.
 */
public abstract class ModuleConfiguration extends YamlConfiguration
{
    /**
     * Creates a module configuration.
     */
    public ModuleConfiguration()
    {
    }

    /**
     * Loads the configuration from disk, merging defaults when supported.
     *
     * @throws IllegalStateException if the configuration cannot be loaded
     */
    public abstract void load();

    /**
     * Saves the configuration to disk.
     *
     * @throws IllegalStateException if the configuration cannot be saved
     */
    public abstract void save();
}
