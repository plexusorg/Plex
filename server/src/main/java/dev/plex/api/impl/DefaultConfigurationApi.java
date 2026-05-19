package dev.plex.api.impl;

import dev.plex.Plex;
import dev.plex.api.config.ConfigurationApi;
import dev.plex.api.config.PlexConfiguration;

final class DefaultConfigurationApi implements ConfigurationApi
{
    private final Plex plugin;

    DefaultConfigurationApi(Plex plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public PlexConfiguration mainConfig()
    {
        return new DefaultPlexConfiguration(plugin.config);
    }

    @Override
    public PlexConfiguration messages()
    {
        return new DefaultPlexConfiguration(plugin.messages);
    }

    @Override
    public PlexConfiguration indefiniteBans()
    {
        return new DefaultPlexConfiguration(plugin.indefBans);
    }

    @Override
    public PlexConfiguration toggles()
    {
        return new DefaultPlexConfiguration(plugin.toggles);
    }
}
