package dev.plex.command.impl;


import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.command.exception.PlayerNotFoundException;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.Punishment;
import dev.plex.api.punishment.PunishmentType;
import dev.plex.util.BanKickUtil;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;


public class BanCMD extends ServerCommand
{
    private final BanRollbackReporter rollbackReporter;

    public BanCMD()
    {
        super(command("ban")
            .description("Bans a player, offline or online")
            .usage("/<command> <player> [message] [-rb]")
            .aliases("offlineban,gtfo")
            .permission("plex.ban")
            .build());
        rollbackReporter = new BanRollbackReporter(plugin);
    }
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context, ServerCommandContext::usage));
        command.then(playerArgument("player")
                .executes(context -> executeCommand(context, commandContext ->
                        ban(commandContext, string(context, "player"), BanReason.parse(null))))
                .then(greedyString("message")
                        .suggests((context, builder) -> suggestOptionalFlags(builder, List.of("-rb")))
                        .executes(context -> executeCommand(context, commandContext -> ban(commandContext,
                                string(context, "player"), BanReason.parse(normalizeGreedyString(string(context, "message"))))))));
    }

    private Component ban(ServerCommandContext context, String playerName, BanReason reason)
    {
        CommandSender sender = context.sender();
        plugin.getPlayerService().findPlayer(playerName).whenComplete((plexPlayer, lookupFailure) ->
        {
            if (lookupFailure != null)
            {
                PlexLog.error("Unable to load player {0}: {1}", playerName, lookupFailure.getMessage());
                sender.sendMessage(Component.text("Unable to load the player."));
                return;
            }
            if (plexPlayer == null)
            {
                sender.sendMessage(PlexUtils.messageComponent("playerNotFound"));
                return;
            }

            BanKickUtil.currentOrLastIp(plugin, plexPlayer).thenCompose(ip ->
                    plugin.getPunishmentManager().isBanned(plexPlayer.getUuid(), ip).thenApply(banned -> new BanState(ip, banned)))
                    .whenComplete((banState, throwable) ->
            {
            if (throwable != null)
            {
                PlexLog.error("Unable to check ban state for {0}: {1}", plexPlayer.getName(), throwable.getMessage());
                sender.sendMessage(Component.text("Unable to check the player's ban state."));
                return;
            }
            if (banState.banned())
            {
                sender.sendMessage(PlexUtils.messageComponent("playerBanned"));
                return;
            }
            Punishment punishment = new Punishment(plexPlayer.getUuid(), context.getUUID(sender));
            punishment.setResolvedPunisherName(context.senderName());
            punishment.setType(PunishmentType.BAN);
            punishment.setReason(reason.text() == null ? PlexUtils.messageString("noReasonProvided") : reason.text());
            punishment.setIp(banState.ip());
            plugin.getPunishmentManager().punish(plexPlayer, punishment).whenComplete((unused, failure) ->
            {
                if (failure != null)
                {
                    PlexLog.error("Unable to ban {0}: {1}", plexPlayer.getName(), failure.getMessage());
                    sender.sendMessage(Component.text("Unable to persist the ban; no action was taken."));
                    return;
                }
                PlexUtils.broadcast(PlexUtils.messageComponent("banningPlayer", context.senderName(), plexPlayer.getName()));
                PlexLog.debug("(From /ban command) PunishedPlayer UUID: " + plexPlayer.getUuid());
                if (reason.rollback()) rollbackReporter.report(sender, plexPlayer.getName());
            });
            });
        });

        return null;
    }

    private record BanState(String ip, boolean banned)
    {
    }

}
