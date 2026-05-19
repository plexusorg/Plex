package dev.plex.api.config;

import dev.plex.module.PlexModule;

public interface ModuleConfigApi
{
    ModuleConfiguration create(PlexModule module, String from, String to);
}
