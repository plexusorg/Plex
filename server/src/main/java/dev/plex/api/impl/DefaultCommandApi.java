package dev.plex.api.impl;

import dev.plex.Plex;
import dev.plex.api.command.CommandApi;
import org.bukkit.command.Command;

final class DefaultCommandApi implements CommandApi
{
    private final Plex plugin;

    DefaultCommandApi(Plex plugin) { this.plugin = plugin; }

    @Override
    public void register(Command command)
    {
        plugin.getServer().getCommandMap().getKnownCommands().remove(command.getName().toLowerCase());
        command.getAliases().forEach(alias -> plugin.getServer().getCommandMap().getKnownCommands().remove(alias.toLowerCase()));
        plugin.getServer().getCommandMap().register("plex", command);
    }

    @Override
    public void unregister(Command command)
    {
        plugin.getServer().getCommandMap().getKnownCommands().remove(command.getName().toLowerCase());
        command.getAliases().forEach(alias -> plugin.getServer().getCommandMap().getKnownCommands().remove(alias.toLowerCase()));
        command.unregister(plugin.getServer().getCommandMap());
    }
}
