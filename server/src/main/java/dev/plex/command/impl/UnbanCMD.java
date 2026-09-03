package dev.plex.command.impl;


import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.command.exception.PlayerNotFoundException;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexUtils;
import dev.plex.util.PlexLog;


import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;


public class UnbanCMD extends ServerCommand
{
    public UnbanCMD()
    {
        super(command("unban")
            .description("Unbans a player, offline or online")
            .usage("/<command> <player>")
            .permission("plex.ban")
            .build());
    }
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context, ServerCommandContext::usage));
        command.then(playerArgument("player")
                .executes(context -> executeCommand(context, commandContext -> executeTyped(commandContext, string(context, "player")))));
    }

    private Component executeTyped(ServerCommandContext context, String playerName)
    {
        CommandSender sender = context.sender();
        plugin.getPlayerService().findPlayer(playerName).whenComplete((target, lookupFailure) ->
        {
            if (lookupFailure != null)
            {
                PlexLog.error("Unable to load player {0}: {1}", playerName, lookupFailure.getMessage());
                sender.sendMessage(Component.text("Unable to load the player."));
                return;
            }
            if (target == null)
            {
                sender.sendMessage(PlexUtils.messageComponent("playerNotFound"));
                return;
            }

            plugin.getPunishmentManager().unban(target.getUuid()).whenComplete((changed, failure) ->
                {
                    if (failure != null)
                    {
                        PlexLog.error("Unable to unban {0}: {1}", target.getUuid(), failure.getMessage());
                        sender.sendMessage(Component.text("Unable to complete the unban; check the server logs."));
                    }
                    else if (!changed) sender.sendMessage(PlexUtils.messageComponent("playerNotBanned"));
                    else PlexUtils.broadcast(PlexUtils.messageComponent("unbanningPlayer", context.senderName(), target.getName()));
                });
        });
        return null;
    }

}
