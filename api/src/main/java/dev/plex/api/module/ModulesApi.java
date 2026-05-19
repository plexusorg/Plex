package dev.plex.api.module;

import dev.plex.module.PlexModuleFile;

import java.util.Collection;
import java.util.Optional;

/**
 * Public module metadata access exposed to modules.
 */
public interface ModulesApi
{
    /**
     * @return immutable metadata for all currently discovered modules
     */
    Collection<PlexModuleFile> loadedModules();

    /**
     * Looks up a module by name.
     *
     * @param name module name from module.yml
     * @return module metadata, if a module with this name is loaded
     */
    Optional<PlexModuleFile> module(String name);
}
