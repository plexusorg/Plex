package dev.plex.api;

/**
 * Describes the module API compatibility level provided by this Plex build.
 */
public interface ApiCompatibility
{
    /**
     * Returns the module API compatibility version.
     *
     * @return the provided module API compatibility version
     */
    int version();
}
