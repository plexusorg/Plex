package dev.plex.api.impl;

import dev.plex.Plex;
import dev.plex.api.module.ModulesApi;
import dev.plex.module.PlexModuleFile;

import java.util.Collection;
import java.util.Locale;
import java.util.Optional;

final class DefaultModulesApi implements ModulesApi
{
    private final Plex plugin;

    DefaultModulesApi(Plex plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public Collection<PlexModuleFile> loadedModules()
    {
        return plugin.getModuleManager().getModules().stream()
                .map(module -> module.getPlexModuleFile())
                .toList();
    }

    @Override
    public Optional<PlexModuleFile> module(String name)
    {
        String normalizedName = name.toLowerCase(Locale.ROOT);
        return loadedModules().stream()
                .filter(module -> module.getName().toLowerCase(Locale.ROOT).equals(normalizedName))
                .findFirst();
    }
}
