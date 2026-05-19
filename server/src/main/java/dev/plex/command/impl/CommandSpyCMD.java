package dev.plex.command.impl;


import dev.plex.command.ServerCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.player.PlexPlayer;

import java.util.Collections;
import java.util.List;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandPermissions(permission = "plex.commandspy", source = RequiredCommandSource.IN_GAME)
@CommandParameters(name = "commandspy", aliases = "cmdspy", description = "Spy on other player's commands")
public class CommandSpyCMD extends ServerCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, @NotNull String[] args)
    {
        if (playerSender != null)
        {
            PlexPlayer plexPlayer = plugin.getPlayerCache().getPlexPlayer(playerSender.getUniqueId());
            plexPlayer.setCommandSpy(!plexPlayer.isCommandSpy());
            plugin.getPlayerService().update(plexPlayer);
            send(sender, messageComponent("toggleCommandSpy")
                    .append(Component.space())
                    .append(plexPlayer.isCommandSpy() ? messageComponent("enabled") : messageComponent("disabled")));
        }
        return null;
    }

    @Override
    public @NotNull List<String> smartTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        return Collections.emptyList();
    }
}
