package dev.plex.api.command;

import dev.plex.command.PlexCommand;

/**
 * Registers and unregisters Plex commands with the running platform.
 */
public interface CommandApi
{
    /**
     * Registers a command with Plex.
     *
     * @param command command to register
     */
    void register(PlexCommand command);

    /**
     * Unregisters a command from Plex.
     *
     * @param command command to unregister
     */
    void unregister(PlexCommand command);
}
