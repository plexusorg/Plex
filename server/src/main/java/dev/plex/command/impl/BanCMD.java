package dev.plex.command.impl;


import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.command.exception.PlayerNotFoundException;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.Punishment;
import dev.plex.punishment.PunishmentType;
import dev.plex.util.BanKickUtil;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import java.util.List;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
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

        plugin.getPunishmentManager().isBanned(plexPlayer.getUuid()).whenComplete((aBoolean, throwable) ->
        {
            plugin.getApi().scheduler().runGlobal(() ->
            {
                if (throwable != null)
                {
                    PlexLog.error("Unable to check ban state for {0}: {1}", plexPlayer.getName(), throwable.getMessage());
                    return;
                }
                if (aBoolean)
                {
                    context.send(sender, context.messageComponent("playerBanned"));
                    return;
                }
                String reason;
                Punishment punishment = new Punishment(plexPlayer.getUuid(), context.getUUID(sender));
                punishment.setResolvedPunisherName(context.senderName());
                punishment.setType(PunishmentType.BAN);
                boolean rollBack = false;
                if (args.length > 1)
                {
                    reason = StringUtils.join(args, " ", 1, args.length);
                    String newReason = StringUtils.normalizeSpace(reason.replace("-rb", ""));
                    punishment.setReason(newReason.trim().isEmpty() ? context.messageString("noReasonProvided") : newReason);
                    rollBack = reason.startsWith("-rb") || reason.endsWith("-rb");
                }
                else
                {
                    punishment.setReason(context.messageString("noReasonProvided"));
                }
                punishment.setEndDate(null);
                punishment.setCustomTime(false);
                punishment.setActive(true);
                punishment.setIp(BanKickUtil.currentOrLastIp(plexPlayer));
                final boolean shouldRollBack = rollBack;
                plugin.getPunishmentManager().punish(plexPlayer, punishment).whenComplete((unused, failure) -> plugin.getApi().scheduler().runGlobal(() ->
                {
                    if (failure != null)
                    {
                        PlexLog.error("Unable to ban {0}: {1}", plexPlayer.getName(), failure.getMessage());
                        context.send(sender, Component.text("Unable to persist the ban; no action was taken."));
                        return;
                    }
                    PlexUtils.broadcast(context.messageComponent("banningPlayer", context.senderName(), plexPlayer.getName()));
                    BanKickUtil.kickPlayersWithIp(plugin, punishment.getIp(),
                            Punishment.generateBanMessage(punishment, plugin.config.getString("banning.ban_url")));
                    PlexLog.debug("(From /ban command) PunishedPlayer UUID: " + plexPlayer.getUuid());
                    if (shouldRollBack) reportRollback(context, sender, plexPlayer.getName());
                }));
            });
        });

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
