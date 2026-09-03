package dev.plex.command.impl;

import dev.plex.util.PlexUtils;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.player.PlexPlayer;


import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CommandSpyCMD extends ServerCommand
{
    public CommandSpyCMD()
    {
        super(command("commandspy")
            .description("Spy on other player's commands")
            .aliases("cmdspy")
            .permission("plex.commandspy")
            .source(RequiredCommandSource.IN_GAME)
            .build());
    }
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context, this::executeTyped));
    }

    private Component executeTyped(ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        Player playerSender = context.player();
        if (playerSender != null)
        {
            PlexPlayer plexPlayer = plugin.getPlayerService().cachedPlayer(playerSender.getUniqueId());
            plexPlayer.setCommandSpy(!plexPlayer.isCommandSpy());
            plugin.getPlayerService().update(plexPlayer);
            sender.sendMessage(PlexUtils.messageComponent("toggleCommandSpy")
                    .append(Component.space())
                    .append(plexPlayer.isCommandSpy() ? PlexUtils.messageComponent("enabled") : PlexUtils.messageComponent("disabled")));
        }
        return null;
    }

}
