package dev.plex.command.impl;

import dev.plex.util.PlexUtils;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexLog;


import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class RemoveLoginMessageCMD extends ServerCommand
{
    public RemoveLoginMessageCMD()
    {
        super(command("removeloginmessage")
            .description("Remove your own (or someone else's) login message")
            .usage("/<command> [-o <player>]")
            .aliases("rlm,removeloginmsg")
            .permission("plex.removeloginmessage")
            .build());
    }
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context, this::removeOwn));
        command.then(literal("-o")
                .requires(source -> canUsePermission(source, "plex.removeloginmessage.others"))
                .executes(context -> executeCommand(context,
                        commandContext -> PlexUtils.messageComponent("specifyPlayer")))
                .then(playerArgument("player")
                        .executes(context -> executeCommand(context,
                                commandContext -> removeOther(commandContext, string(context, "player"))))));
    }

    private Component removeOwn(ServerCommandContext context)
    {
        Player playerSender = context.player();
        if (playerSender != null)
        {
            PlexPlayer plexPlayer = plugin.getPlayerService().cachedPlayer(playerSender.getUniqueId());
            plexPlayer.setLoginMessage("");
            plugin.getPlayerService().update(plexPlayer).whenComplete((unused, failure) ->
            {
                if (failure != null)
                {
                    PlexLog.warn("Unable to remove login message for {0}: {1}", plexPlayer.getUuid(), failure.getMessage());
                    context.sender().sendMessage(Component.text("Unable to save the login message."));
                }
                else context.sender().sendMessage(PlexUtils.messageComponent("removedOwnLoginMessage"));
            });
            return null;
        }
        return PlexUtils.messageComponent("noPermissionConsole");
    }

    private Component removeOther(ServerCommandContext context, String playerName)
    {
        context.checkPermission(context.sender(), "plex.removeloginmessage.others");
        plugin.getPlayerService().findPlayer(playerName).whenComplete((plexPlayer, failure) ->
        {
            if (failure != null)
            {
                PlexLog.warn("Unable to load player {0}: {1}", playerName, failure.getMessage());
                context.sender().sendMessage(Component.text("Unable to load the player."));
                return;
            }
            if (plexPlayer == null)
            {
                context.sender().sendMessage(PlexUtils.messageComponent("playerNotFound"));
                return;
            }
            plexPlayer.setLoginMessage("");
            plugin.getPlayerService().update(plexPlayer).whenComplete((unused, updateFailure) ->
            {
                if (updateFailure != null)
                {
                    PlexLog.warn("Unable to remove login message for {0}: {1}", plexPlayer.getUuid(), updateFailure.getMessage());
                    context.sender().sendMessage(Component.text("Unable to save the login message."));
                }
                else context.sender().sendMessage(PlexUtils.messageComponent("removedOtherLoginMessage", plexPlayer.getName()));
            });
        });
        return null;
    }

}
