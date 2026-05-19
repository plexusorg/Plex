package dev.plex.api.impl;

import dev.plex.api.config.ModuleConfigApi;
import dev.plex.api.config.ModuleConfiguration;
import dev.plex.module.PlexModule;

final class DefaultModuleConfigApi implements ModuleConfigApi
{
    @Override
    public ModuleConfiguration create(PlexModule module, String from, String to)
    {
        return new ServerModuleConfiguration(module, from, to);
    }
}
