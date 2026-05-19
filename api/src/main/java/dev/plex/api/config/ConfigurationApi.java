package dev.plex.api.config;

/**
 * Public configuration access exposed to modules.
 */
public interface ConfigurationApi
{
    PlexConfiguration mainConfig();

    PlexConfiguration messages();

    PlexConfiguration indefiniteBans();

    PlexConfiguration toggles();
}
