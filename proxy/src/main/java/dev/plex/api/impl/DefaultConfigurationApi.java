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
        return plugin.getConfig();
    }

    @Override
    public PlexConfiguration messages()
    {
        return plugin.getMessages();
    }

    @Override
    public PlexConfiguration indefiniteBans()
    {
        throw new UnsupportedOperationException("Proxy does not provide indefinite bans configuration");
    }

    @Override
    public PlexConfiguration toggles()
    {
        throw new UnsupportedOperationException("Proxy does not provide toggles configuration");
    }
}
