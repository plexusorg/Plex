package dev.plex.api.command;

import dev.plex.command.PlexCommand;

public interface CommandApi
{
    void register(PlexCommand command);

    void unregister(PlexCommand command);
}
