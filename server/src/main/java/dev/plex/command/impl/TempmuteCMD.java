package dev.plex.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.Punishment;
import dev.plex.api.punishment.PunishmentType;
import dev.plex.util.PlexUtils;
import dev.plex.util.TimeUtils;
import dev.plex.util.PlexLog;

import java.time.ZonedDateTime;
import java.util.Arrays;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TempmuteCMD extends ServerCommand
{
    public TempmuteCMD()
    {
        super(command("tempmute")
            .description("Temporarily mute a player on the server")
            .usage("/<command> <player> <time> [reason]")
            .aliases("tmute")
            .permission("plex.tempmute")
            .build());
    }
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context, ServerCommandContext::usage));
        command.then(playerArgument("player")
                .then(word("time")
                        .executes(context -> executeCommand(context, commandContext -> tempmute(commandContext,
                                string(context, "player"), string(context, "time"), null)))
                        .then(greedyString("reason")
                                .executes(context -> executeCommand(context, commandContext -> tempmute(commandContext,
                                        string(context, "player"), string(context, "time"),
                                        normalizeGreedyString(string(context, "reason"))))))));
    }

    private Component tempmute(ServerCommandContext context, String playerName, String time, String suppliedReason)
    {
        CommandSender sender = context.sender();
        Player player = getNonNullPlayer(playerName);
        PlexPlayer punishedPlayer = getCachedPlexPlayer(player.getUniqueId());

        if (plugin.getPunishmentManager().hasActivePunishment(punishedPlayer, PunishmentType.MUTE))
        {
            return PlexUtils.messageComponent("playerMuted");
        }

        if (context.silentCheckPermission(player, "plex.tempmute"))
        {
            sender.sendMessage(PlexUtils.messageComponent("higherRankThanYou"));
            return null;
        }

        ZonedDateTime endDate;
        try
        {
            endDate = TimeUtils.createDate(time);
        }
        catch (NumberFormatException e)
        {
            return PlexUtils.messageComponent("invalidTimeFormat");
        }

        if (endDate.isBefore(ZonedDateTime.now()))
        {
            return PlexUtils.messageComponent("timeMustBeFuture");
        }

        ZonedDateTime oneWeekFromNow = ZonedDateTime.now().plus(PunishmentType.MUTE.maximumDuration().orElseThrow());
        if (endDate.isAfter(oneWeekFromNow))
        {
            return PlexUtils.messageComponent("maxTimeExceeded");
        }

        final String reason = suppliedReason == null ? PlexUtils.messageString("noReasonProvided") : suppliedReason;

        Punishment punishment = new Punishment(punishedPlayer.getUuid(), context.getUUID(sender));
        punishment.setEndDate(endDate);
        punishment.setType(PunishmentType.MUTE);
        punishment.setIp(player.getAddress().getAddress().getHostAddress().trim());
        punishment.setReason(reason);

        plugin.getPunishmentManager().punish(punishedPlayer, punishment).whenComplete((unused, failure) ->
        {
            if (failure != null)
            {
                PlexLog.error("Unable to tempmute {0}: {1}", punishedPlayer.getUuid(), failure.getMessage());
                sender.sendMessage(Component.text("Unable to persist the mute; no action was taken."));
            }
            else PlexUtils.broadcast(PlexUtils.messageComponent("tempMutedPlayer", context.senderName(), player.getName(), TimeUtils.formatRelativeTime(endDate)));
        });
        return null;
    }

}
