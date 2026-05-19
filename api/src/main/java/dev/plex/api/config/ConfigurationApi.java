package dev.plex.api.config;

/**
 * Public configuration access exposed to modules.
 */
public interface ConfigurationApi
{
    /**
     * Returns the main Plex configuration.
     *
     * @return the main Plex configuration
     */
    PlexConfiguration mainConfig();

    /**
     * Returns the shared message configuration.
     *
     * @return the shared message configuration
     */
    PlexConfiguration messages();

    /**
     * Returns the indefinite ban configuration.
     *
     * @return the indefinite ban configuration
     */
    PlexConfiguration indefiniteBans();

    /**
     * Returns the toggle configuration.
     *
     * @return the toggle configuration
     */
    PlexConfiguration toggles();
}
