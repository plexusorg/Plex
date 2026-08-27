package dev.plex.api.impl;

import dev.plex.Plex;
import dev.plex.api.command.CommandApi;
import dev.plex.api.command.CommandExecutionIdentity;
import dev.plex.command.PlexCommand;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;

final class DefaultCommandApi implements CommandApi
{
    private final Plex plugin;

    DefaultCommandApi(Plex plugin) { this.plugin = plugin; }

    @Override
    public void register(PlexCommand command)
    {
        command.bindApi(plugin.getApi());
        if (plugin.getCommandHandler() == null)
        {
            plugin.getPendingCommands().add(command);
            return;
        }
        plugin.getCommandHandler().registerCommand(command);
    }

    @Override
    public void unregister(PlexCommand command)
    {
        plugin.getPendingCommands().remove(command);
        if (plugin.getCommandHandler() != null)
        {
            plugin.getCommandHandler().unregisterCommand(command);
        }
    }

    @Override
    public List<PlexCommand> registeredCommands()
    {
        if (plugin.getCommandHandler() == null)
        {
            return List.copyOf(plugin.getPendingCommands());
        }
        return List.copyOf(plugin.getCommandHandler().getCommands());
    }

    @Override
    public boolean requiresLifecycleReload()
    {
        return plugin.getCommandHandler() != null && plugin.getCommandHandler().requiresLifecycleReload();
    }

    @Override
    public boolean dispatchAsConsole(UUID identityId, String identityName, String command, Consumer<? super Component> feedback)
    {
        return CommandExecutionIdentity.call(
                identityId,
                identityName,
                () -> Bukkit.dispatchCommand(Bukkit.createCommandSender(feedback), command));
    }
}
