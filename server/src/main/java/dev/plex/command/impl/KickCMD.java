package dev.plex.command.impl;


import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.command.exception.PlayerNotFoundException;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.Punishment;
import dev.plex.punishment.PunishmentType;
import dev.plex.util.BungeeUtil;
import dev.plex.util.PlexUtils;
import dev.plex.util.TimeUtils;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
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
        command.executes(context -> executeCommand(context));
        command.then(playerArgument("player")
                .executes(context -> executeCommand(context, string(context, "player")))
                .then(greedyString("reason")
                        .executes(context -> executeCommand(context, argsWithGreedy(string(context, "player"), string(context, "reason"))))));
    }

    @Override
    protected Component execute(@NotNull ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        Player playerSender = context.player();
        String[] args = context.args();
        if (args.length == 0)
        {
            return context.usage();
        }

        PlexPlayer plexPlayer = plugin.getPlayerService().getPlayer(args[0]);
        String reason = context.messageString("noReasonProvided");

        if (plexPlayer == null)
        {
            throw new PlayerNotFoundException();
        }
        Player player = Bukkit.getPlayer(plexPlayer.getUuid());

        if (player == null)
        {
            throw new PlayerNotFoundException();
        }
        Punishment punishment = new Punishment(plexPlayer.getUuid(), context.getUUID(sender));
        punishment.setType(PunishmentType.KICK);
        if (args.length > 1)
        {
            reason = StringUtils.join(args, " ", 1, args.length);
        }

        punishment.setReason(reason);
        punishment.setPunishedUsername(plexPlayer.getName());
        punishment.setEndDate(ZonedDateTime.now(ZoneId.of(TimeUtils.TIMEZONE)));
        punishment.setCustomTime(false);
        punishment.setActive(false);
        punishment.setIp(player.getAddress().getAddress().getHostAddress().trim());
        plugin.getPunishmentManager().punish(plexPlayer, punishment);
        PlexUtils.broadcast(context.messageComponent("kickedPlayer", sender.getName(), plexPlayer.getName()));
        BungeeUtil.kickPlayer(plugin, player, Punishment.generateKickMessage(punishment, plugin.getPlayerService()));
        return null;
    }

}
