package dev.plex.command.impl;


import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.command.exception.CommandFailException;
import dev.plex.command.exception.PlayerNotFoundException;
import dev.plex.player.PlexPlayer;
import dev.plex.api.punishment.PunishmentType;
import dev.plex.util.PlexUtils;
import dev.plex.util.PlexLog;


import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class UnmuteCMD extends ServerCommand
{
    public UnmuteCMD()
    {
        super(command("unmute")
            .description("Unmute a player")
            .usage("/<command> <player>")
            .aliases("eunmute")
            .permission("plex.unmute")
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
        plugin.getPlayerService().findPlayer(playerName).whenComplete((punishedPlayer, lookupFailure) ->
        {
        if (lookupFailure != null)
        {
            PlexLog.error("Unable to load player {0}: {1}", playerName, lookupFailure.getMessage());
            sender.sendMessage(Component.text("Unable to load the player."));
            return;
        }
        if (punishedPlayer == null)
        {
            sender.sendMessage(PlexUtils.messageComponent("playerNotFound"));
            return;
        }

        if (!plugin.getPunishmentManager().hasActivePunishment(punishedPlayer, PunishmentType.MUTE))
        {
            throw new CommandFailException(PlexUtils.messageString("playerNotMuted"));
        }
        plugin.getPunishmentManager().deactivateTimedPunishment(punishedPlayer, PunishmentType.MUTE)
                .whenComplete((unused, failure) ->
                {
                    if (failure != null)
                    {
                        PlexLog.error("Unable to unmute {0}: {1}", punishedPlayer.getUuid(), failure.getMessage());
                        sender.sendMessage(Component.text("Unable to persist the unmute; no action was taken."));
                    }
                    else PlexUtils.broadcast(PlexUtils.messageComponent("unmutedPlayer", context.senderName(), punishedPlayer.getName()));
                });
        });
        return null;
    }

}
