package dev.plex.command.impl;

import static dev.plex.api.message.MessagePlaceholder.placeholder;


import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.command.exception.PlayerNotFoundException;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.Punishment;
import dev.plex.api.punishment.PunishmentType;
import dev.plex.util.BanKickUtil;
import dev.plex.util.PlexUtils;
import dev.plex.util.PlexLog;
import dev.plex.util.TimeUtils;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;


public class TempbanCMD extends ServerCommand
{
    private final BanRollbackReporter rollbackReporter;

    public TempbanCMD()
    {
        super(command("tempban")
            .description("Temporarily ban a player")
            .usage("/<command> <player> <time> [message] [-rb]")
            .permission("plex.tempban")
            .build());
        rollbackReporter = new BanRollbackReporter(plugin);
    }
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context, ServerCommandContext::usage));
        command.then(playerArgument("player")
                .then(word("time")
                        .executes(context -> executeCommand(context, commandContext -> tempban(commandContext,
                                string(context, "player"), string(context, "time"), BanReason.parse(null))))
                        .then(greedyString("message")
                                .suggests((context, builder) -> suggestOptionalFlags(builder, List.of("-rb")))
                                .executes(context -> executeCommand(context, commandContext -> tempban(commandContext,
                                        string(context, "player"), string(context, "time"),
                                        BanReason.parse(normalizeGreedyString(string(context, "message")))))))));
    }

    private Component tempban(ServerCommandContext context, String playerName, String time, BanReason reason)
    {
        CommandSender sender = context.sender();
        final java.time.ZonedDateTime endDate;
        try
        {
            endDate = TimeUtils.createDate(time);
        }
        catch (NumberFormatException e)
        {
            return PlexUtils.messageComponent("invalidTimeFormat");
        }
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
        BanKickUtil.currentOrLastIp(plugin, target).thenCompose(ip ->
                plugin.getPunishmentManager().isBanned(target.getUuid(), ip).thenApply(banned -> new BanState(ip, banned)))
                .whenComplete((banState, checkFailure) ->
        {
            if (checkFailure != null)
            {
                PlexLog.error("Unable to check ban state for {0}: {1}", target.getName(), checkFailure.getMessage());
                sender.sendMessage(Component.text("Unable to check the player's ban state."));
                return;
            }
            if (banState.banned())
            {
                sender.sendMessage(PlexUtils.messageComponent("playerBanned"));
                return;
            }
            Punishment punishment = new Punishment(target.getUuid(), context.getUUID(sender));
            punishment.setResolvedPunisherName(context.senderName());
            punishment.setType(PunishmentType.TEMPBAN);
            punishment.setReason(reason.text() == null ? PlexUtils.messageString("noReasonProvided") : reason.text());
            punishment.setEndDate(endDate);
            punishment.setIp(banState.ip());
            plugin.getPunishmentManager().punish(target, punishment).whenComplete((unused, failure) ->
            {
                if (failure != null)
                {
                    PlexLog.error("Unable to tempban {0}: {1}", target.getName(), failure.getMessage());
                    sender.sendMessage(Component.text("Unable to complete the tempban; check the server logs."));
                    return;
                }
                PlexUtils.broadcast(PlexUtils.messageComponent("banningPlayer", placeholder("sender", context.senderName()), placeholder("player", target.getName())));
                if (reason.rollback()) rollbackReporter.report(sender, target.getName());
            });
        });
        });
        return null;
    }

    private record BanState(String ip, boolean banned)
    {
    }

}
