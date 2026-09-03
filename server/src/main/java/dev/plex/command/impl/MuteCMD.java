package dev.plex.command.impl;

import org.bukkit.Bukkit;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.Punishment;
import dev.plex.api.punishment.PunishmentType;
import dev.plex.util.PlexUtils;
import dev.plex.util.BanKickUtil;
import dev.plex.util.TimeUtils;
import dev.plex.util.PlexLog;

import java.time.ZonedDateTime;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MuteCMD extends ServerCommand
{
    public MuteCMD()
    {
        super(command("mute")
            .description("Mute a player on the server")
            .usage("/<command> <player>")
            .aliases("stfu,emute,silence,esilence")
            .permission("plex.mute")
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
        Player player = getNonNullPlayer(playerName);
        PlexPlayer punishedPlayer = getCachedPlexPlayer(player.getUniqueId());

        if (plugin.getPunishmentManager().hasActivePunishment(punishedPlayer, PunishmentType.MUTE))
        {
            return PlexUtils.messageComponent("playerMuted");
        }

        if (context.silentCheckPermission(player, "plex.mute"))
        {
            sender.sendMessage(PlexUtils.messageComponent("higherRankThanYou"));
            return null;
        }

        Punishment punishment = new Punishment(punishedPlayer.getUuid(), context.getUUID(sender));
        ZonedDateTime date = ZonedDateTime.now(TimeUtils.zoneId());
        punishment.setEndDate(date.plusSeconds(plugin.config.getInt("punishments.mute-timer", 300)));
        punishment.setType(PunishmentType.MUTE);
        punishment.setReason("");

        BanKickUtil.currentOrLastIp(plugin, punishedPlayer).thenCompose(ip ->
        {
            punishment.setIp(ip);
            return plugin.getPunishmentManager().punish(punishedPlayer, punishment);
        }).whenComplete((unused, failure) ->
        {
            if (failure != null)
            {
                PlexLog.error("Unable to mute {0}: {1}", punishedPlayer.getUuid(), failure.getMessage());
                sender.sendMessage(Component.text("Unable to persist the mute; no action was taken."));
            }
            else PlexUtils.broadcast(PlexUtils.messageComponent("mutedPlayer", context.senderName(), player.getName()));
        });
        return null;
    }

}
