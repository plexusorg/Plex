package dev.plex.api.impl;

import dev.plex.api.module.ModulesApi;
import dev.plex.module.PlexModuleFile;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

final class DefaultModulesApi implements ModulesApi
{
    @Override
    public Collection<PlexModuleFile> loadedModules()
    {
        return List.of();
    }

    @Override
    public Optional<PlexModuleFile> module(String name)
    {
        return Optional.empty();
    }
}
