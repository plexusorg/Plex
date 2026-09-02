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
    public BanCMD()
    {
        super(command("ban")
            .description("Bans a player, offline or online")
            .usage("/<command> <player> [message] [-rb]")
            .aliases("offlineban,gtfo")
            .permission("plex.ban")
            .build());
    }
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context));
        command.then(playerArgument("player")
                .executes(context -> executeCommand(context, string(context, "player")))
                .then(greedyString("message")
                        .suggests((context, builder) -> suggestOptionalFlags(builder, List.of("-rb")))
                        .executes(context -> executeCommand(context, argsWithGreedy(string(context, "player"), string(context, "message"))))));
    }

    @Override
    protected Component execute(@NotNull ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        String[] args = context.args();
        if (args.length == 0)
        {
            return context.usage();
        }

        final PlexPlayer plexPlayer = plugin.getPlayerService().getPlayer(args[0]);

        if (plexPlayer == null)
        {
            throw new PlayerNotFoundException();
        }

        plugin.getPunishmentManager().isBanned(plexPlayer.getUuid(), BanKickUtil.currentOrLastIp(plexPlayer)).whenComplete((aBoolean, throwable) ->
        {
            if (throwable != null)
            {
                PlexLog.error("Unable to check ban state for {0}: {1}", plexPlayer.getName(), throwable.getMessage());
                context.send(sender, Component.text("Unable to check the player's ban state."));
                return;
            }
            if (aBoolean)
            {
                context.send(sender, context.messageComponent("playerBanned"));
                return;
            }
            Punishment punishment = new Punishment(plexPlayer.getUuid(), context.getUUID(sender));
            punishment.setResolvedPunisherName(context.senderName());
            punishment.setType(PunishmentType.BAN);
            boolean rollBack = false;
            punishment.setReason(context.messageString("noReasonProvided"));
            if (args.length > 1)
            {
                List<String> reason = new ArrayList<>(Arrays.asList(args).subList(1, args.length));
                rollBack = reason.getFirst().equals("-rb") || reason.getLast().equals("-rb");
                if (reason.getFirst().equals("-rb")) reason.removeFirst();
                if (!reason.isEmpty() && reason.getLast().equals("-rb")) reason.removeLast();
                if (!reason.isEmpty()) punishment.setReason(String.join(" ", reason));
            }
            punishment.setIp(BanKickUtil.currentOrLastIp(plexPlayer));
            final boolean shouldRollBack = rollBack;
            plugin.getPunishmentManager().punish(plexPlayer, punishment).whenComplete((unused, failure) ->
            {
                if (failure != null)
                {
                    PlexLog.error("Unable to ban {0}: {1}", plexPlayer.getName(), failure.getMessage());
                    context.send(sender, Component.text("Unable to persist the ban; no action was taken."));
                    return;
                }
                PlexUtils.broadcast(context.messageComponent("banningPlayer", context.senderName(), plexPlayer.getName()));
                PlexLog.debug("(From /ban command) PunishedPlayer UUID: " + plexPlayer.getUuid());
                if (shouldRollBack) reportRollback(context, sender, plexPlayer.getName());
            });
        });

        return null;
    }

    private void reportRollback(ServerCommandContext context, CommandSender sender, String playerName)
    {
        plugin.getApi().rollback().rollbackLastDay(sender, playerName).whenComplete((count, failure) ->
        {
            if (failure != null)
            {
                PlexLog.error("Unable to rollback {0}: {1}", playerName, failure.getMessage());
                context.send(sender, context.messageComponent("prismRollbackError", failure.getMessage()));
            }
            else if (count == 0) context.send(sender, context.messageComponent("prismNoResult", count));
            else context.send(sender, context.messageComponent("prismRollbackMessage", count));
        });
    }

}
