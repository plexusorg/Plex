package dev.plex.command.impl;

import static dev.plex.api.message.MessagePlaceholder.placeholder;


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

public class UnfreezeCMD extends ServerCommand
{
    public UnfreezeCMD()
    {
        super(command("unfreeze")
            .description("Unfreeze a player")
            .usage("/<command> <player>")
            .permission("plex.unfreeze")
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

        if (!plugin.getPunishmentManager().hasActivePunishment(punishedPlayer, PunishmentType.FREEZE))
        {
            throw new CommandFailException(PlexUtils.messageString("playerNotFrozen"));
        }
        plugin.getPunishmentManager().deactivateTimedPunishment(punishedPlayer, PunishmentType.FREEZE)
                .whenComplete((unused, failure) ->
                {
                    if (failure != null)
                    {
                        PlexLog.error("Unable to unfreeze {0}: {1}", punishedPlayer.getUuid(), failure.getMessage());
                        sender.sendMessage(Component.text("Unable to persist the unfreeze; no action was taken."));
                    }
                    else PlexUtils.broadcast(PlexUtils.messageComponent("unfrozePlayer", placeholder("sender", context.senderName()), placeholder("player", punishedPlayer.getName())));
                });
        });
        return null;
    }

}
