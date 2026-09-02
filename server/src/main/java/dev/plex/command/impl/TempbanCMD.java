package dev.plex.command.impl;


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

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;


public class TempbanCMD extends ServerCommand
{
    public TempbanCMD()
    {
        super(command("tempban")
            .description("Temporarily ban a player")
            .usage("/<command> <player> <time> [message] [-rb]")
            .permission("plex.tempban")
            .build());
    }
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context));
        command.then(playerArgument("player")
                .then(word("time")
                        .executes(context -> executeCommand(context, string(context, "player"), string(context, "time")))
                        .then(greedyString("message")
                                .suggests((context, builder) -> suggestOptionalFlags(builder, List.of("-rb")))
                                .executes(context -> executeCommand(context, argsWithGreedy(string(context, "player"), string(context, "time"), string(context, "message")))))));
    }

    @Override
    protected Component execute(@NotNull ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        String[] args = context.args();
        if (args.length <= 1)
        {
            return context.usage();
        }

        PlexPlayer target = plugin.getPlayerService().getPlayer(args[0]);
        String reason;

        if (target == null)
        {
            throw new PlayerNotFoundException();
        }
        Punishment punishment = new Punishment(target.getUuid(), context.getUUID(sender));
        punishment.setResolvedPunisherName(context.senderName());
        punishment.setType(PunishmentType.TEMPBAN);
        boolean rollBack = false;
        if (args.length > 2)
        {
            reason = StringUtils.join(args, " ", 2, args.length);
            String newReason = StringUtils.normalizeSpace(reason.replace("-rb", ""));
            punishment.setReason(newReason.trim().isEmpty() ? context.messageString("noReasonProvided") : newReason);
            rollBack = reason.startsWith("-rb") || reason.endsWith("-rb");
        }
        else
        {
            punishment.setReason(context.messageString("noReasonProvided"));
        }
        try
        {
            punishment.setEndDate(TimeUtils.createDate(args[1]));
        }
        catch (NumberFormatException e)
        {
            return context.messageComponent("invalidTimeFormat");
        }
        punishment.setIp(BanKickUtil.currentOrLastIp(target));
        final boolean shouldRollBack = rollBack;
        plugin.getPunishmentManager().isBanned(target.getUuid(), punishment.getIp()).whenComplete((banned, checkFailure) ->
                plugin.getApi().scheduler().runGlobal(() ->
        {
            if (checkFailure != null)
            {
                PlexLog.error("Unable to check ban state for {0}: {1}", target.getName(), checkFailure.getMessage());
                context.send(sender, Component.text("Unable to check the player's ban state."));
                return;
            }
            if (banned)
            {
                context.send(sender, context.messageComponent("playerBanned"));
                return;
            }
            plugin.getPunishmentManager().punish(target, punishment).whenComplete((unused, failure) ->
                    plugin.getApi().scheduler().runGlobal(() ->
            {
                if (failure != null)
                {
                    PlexLog.error("Unable to tempban {0}: {1}", target.getName(), failure.getMessage());
                    context.send(sender, Component.text("Unable to persist the ban; no action was taken."));
                    return;
                }
                PlexUtils.broadcast(context.messageComponent("banningPlayer", context.senderName(), target.getName()));
                if (shouldRollBack) reportRollback(context, sender, target.getName());
            }));
        }));
        return null;
    }

    private void reportRollback(ServerCommandContext context, CommandSender sender, String playerName)
    {
        plugin.getApi().rollback().rollbackLastDay(sender, playerName).whenComplete((count, failure) ->
                plugin.getApi().scheduler().runGlobal(() ->
                {
                    if (failure != null)
                    {
                        PlexLog.error("Unable to rollback {0}: {1}", playerName, failure.getMessage());
                        context.send(sender, context.messageComponent("prismRollbackError", failure.getMessage()));
                    }
                    else if (count == 0) context.send(sender, context.messageComponent("prismNoResult", count));
                    else context.send(sender, context.messageComponent("prismRollbackMessage", count));
                }));
    }

}
