package dev.plex.api.module;

import dev.plex.module.PlexModuleFile;

import java.util.Collection;
import java.util.Optional;

/**
 * Provides information about loaded modules.
 */
public interface ModulesApi
{
    /**
     * Returns information about all loaded modules.
     *
     * @return immutable information about all loaded modules
     */
    Collection<PlexModuleFile> loadedModules();

    /**
     * Looks up a module by name.
     *
     * @param name module name from module.yml
     * @return module information, if a module with this name is loaded
     */
    Optional<PlexModuleFile> module(String name);
}
