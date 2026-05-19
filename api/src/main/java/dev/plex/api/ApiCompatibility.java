package dev.plex.api;

/**
 * Describes the module API compatibility level provided by this Plex build.
 */
public interface ApiCompatibility
{
    /**
     * @return the provided module API compatibility version
     */
    int version();
}
