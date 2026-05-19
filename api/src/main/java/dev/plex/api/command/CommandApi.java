package dev.plex.api.command;

import org.bukkit.command.Command;

public interface CommandApi
{
    void register(Command command);

    void unregister(Command command);
}
