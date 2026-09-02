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

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class FreezeCMD extends ServerCommand
{
    public FreezeCMD()
    {
        super(command("freeze")
            .description("Freeze a player on the server")
            .usage("/<command> <player>")
            .aliases("fr")
            .permission("plex.freeze")
            .build());
    }
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context));
        command.then(playerArgument("player")
                .executes(context -> executeCommand(context, string(context, "player"))));
    }

    @Override
    protected Component execute(@NotNull ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        String[] args = context.args();
        if (args.length != 1)
        {
            return context.usage();
        }
        Player player = context.getNonNullPlayer(args[0]);
        PlexPlayer punishedPlayer = context.getPlexPlayer(player);

        if (plugin.getPunishmentManager().hasActivePunishment(punishedPlayer, PunishmentType.FREEZE))
        {
            return context.messageComponent("playerFrozen");
        }

        Punishment punishment = new Punishment(punishedPlayer.getUuid(), context.getUUID(sender));
        ZonedDateTime date = ZonedDateTime.now(TimeUtils.zoneId());
        punishment.setEndDate(date.plusSeconds(plugin.config.getInt("punishments.freeze-timer", 300)));
        punishment.setType(PunishmentType.FREEZE);
        punishment.setIp(player.getAddress().getAddress().getHostAddress().trim());
        punishment.setReason("");

        plugin.getPunishmentManager().punish(punishedPlayer, punishment).whenComplete((unused, failure) ->
        {
            if (failure != null)
            {
                PlexLog.error("Unable to freeze {0}: {1}", punishedPlayer.getUuid(), failure.getMessage());
                context.send(sender, Component.text("Unable to persist the freeze; no action was taken."));
            }
            else PlexUtils.broadcast(context.messageComponent("frozePlayer", context.senderName(), player.getName()));
        });
        return null;
    }

}
