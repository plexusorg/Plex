package dev.plex.api.impl;

import dev.plex.Plex;
import dev.plex.api.command.CommandApi;
import dev.plex.command.PlexCommand;
import dev.plex.util.PlexLog;

final class DefaultCommandApi implements CommandApi
{
    private final Plex plugin;

    DefaultCommandApi(Plex plugin) { this.plugin = plugin; }

    @Override
    public void register(PlexCommand command)
    {
        if (plugin.getCommandHandler() == null)
        {
            plugin.getPendingCommands().add(command);
            PlexLog.warn("Command {0} was registered before the command handler initialized; queueing it for Brigadier registration.", command.getName());
            return;
        }
        plugin.getCommandHandler().registerCommand(command);
    }

    @Override
    public void unregister(PlexCommand command)
    {
        if (plugin.getCommandHandler() != null)
        {
            plugin.getCommandHandler().unregisterCommand(command);
        }
    }
}
