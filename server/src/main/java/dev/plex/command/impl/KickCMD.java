package dev.plex.command.impl;

import org.bukkit.Bukkit;


import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.command.exception.PlayerNotFoundException;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.Punishment;
import dev.plex.api.punishment.PunishmentType;
import dev.plex.util.BungeeUtil;
import dev.plex.util.PlexUtils;
import dev.plex.util.TimeUtils;
import dev.plex.util.PlexLog;

import java.time.ZonedDateTime;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class KickCMD extends ServerCommand
{
    public KickCMD()
    {
        super(command("kick")
            .description("Kicks a player")
            .usage("/<command> <player>")
            .aliases("ekick")
            .permission("plex.kick")
            .build());
    }
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context, ServerCommandContext::usage));
        command.then(playerArgument("player")
                .executes(context -> executeCommand(context, commandContext -> executeTyped(commandContext, string(context, "player"), null)))
                .then(greedyString("reason")
                        .executes(context -> executeCommand(context, commandContext -> executeTyped(commandContext,
                                string(context, "player"), normalizeGreedyString(string(context, "reason")))))));
    }

    private Component executeTyped(ServerCommandContext context, String playerName, String suppliedReason)
    {
        String reason = suppliedReason == null ? PlexUtils.messageString("noReasonProvided") : suppliedReason;
        Player player = getNonNullPlayer(playerName);
        player.getScheduler().run(plugin, task -> kick(context, player, reason),
                () -> context.sender().sendMessage(Component.text("The player disconnected before the kick could be prepared.")));
        return null;
    }

    private void kick(ServerCommandContext context, Player player, String reason)
    {
        CommandSender sender = context.sender();
        PlexPlayer plexPlayer = plugin.getPlayerService().cachedPlayer(player.getUniqueId());
        if (plexPlayer == null)
        {
            sender.sendMessage(PlexUtils.messageComponent("playerNotFound"));
            return;
        }
        Punishment punishment = new Punishment(plexPlayer.getUuid(), context.getUUID(sender));
        punishment.setResolvedPunisherName(context.senderName());
        punishment.setType(PunishmentType.KICK);
        punishment.setReason(reason);
        punishment.setIp(player.getAddress().getAddress().getHostAddress().trim());
        plugin.getPunishmentManager().punish(plexPlayer, punishment).whenComplete((unused, failure) ->
        {
            if (failure != null)
            {
                PlexLog.error("Unable to persist kick for {0}: {1}", plexPlayer.getUuid(), failure.getMessage());
                sender.sendMessage(Component.text("Unable to persist the kick; no action was taken."));
                return;
            }
            player.getScheduler().run(plugin, task ->
            {
                BungeeUtil.kickPlayer(plugin, player, Punishment.generateKickMessage(punishment));
                PlexUtils.broadcast(PlexUtils.messageComponent("kickedPlayer", context.senderName(), plexPlayer.getName()));
            }, () -> sender.sendMessage(Component.text("The kick was persisted, but the player disconnected before it could be applied.")));
        });
    }

}
