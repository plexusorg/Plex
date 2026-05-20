package dev.plex.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.Punishment;
import dev.plex.punishment.PunishmentType;
import dev.plex.util.PlexUtils;
import dev.plex.util.TimeUtils;

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
        command.executes(context -> executeCommand(context));
        command.then(playerArgument("player")
                .then(word("time")
                        .executes(context -> executeCommand(context, string(context, "player"), string(context, "time")))
                        .then(greedyString("reason")
                                .executes(context -> executeCommand(context, argsWithGreedy(string(context, "player"), string(context, "time"), string(context, "reason")))))));
    }

    @Override
    protected Component execute(@NotNull ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        Player playerSender = context.player();
        String[] args = context.args();
        if (args.length < 2)
        {
            return context.usage();
        }

        Player player = context.getNonNullPlayer(args[0]);
        PlexPlayer punishedPlayer = context.getOfflinePlexPlayer(player.getUniqueId());

        if (punishedPlayer.isMuted())
        {
            return context.messageComponent("playerMuted");
        }

        if (context.silentCheckPermission(player, "plex.tempmute"))
        {
            context.send(sender, context.messageComponent("higherRankThanYou"));
            return null;
        }

        ZonedDateTime endDate;
        try
        {
            endDate = TimeUtils.createDate(args[1]);
        }
        catch (NumberFormatException e)
        {
            return context.messageComponent("invalidTimeFormat");
        }

        if (endDate.isBefore(ZonedDateTime.now()))
        {
            return context.messageComponent("timeMustBeFuture");
        }

        ZonedDateTime oneWeekFromNow = ZonedDateTime.now().plusWeeks(1);
        if (endDate.isAfter(oneWeekFromNow))
        {
            return context.messageComponent("maxTimeExceeded");
        }

        final String reason = args.length >= 3 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length))
                : context.messageString("noReasonProvided");

        Punishment punishment = new Punishment(punishedPlayer.getUuid(), context.getUUID(sender));
        punishment.setCustomTime(true);
        punishment.setEndDate(endDate);
        punishment.setType(PunishmentType.MUTE);
        punishment.setPunishedUsername(player.getName());
        punishment.setIp(player.getAddress().getAddress().getHostAddress().trim());
        punishment.setReason(reason);
        punishment.setActive(true);

        plugin.getPunishmentManager().punish(punishedPlayer, punishment);
        PlexUtils.broadcast(context.messageComponent("tempMutedPlayer", sender.getName(), player.getName(), TimeUtils.formatRelativeTime(endDate)));
        return null;
    }

}
