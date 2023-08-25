package dev.plex.command.impl;

import dev.plex.cache.DataUtils;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.PlayerNotFoundException;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.Punishment;
import dev.plex.punishment.PunishmentType;
import dev.plex.rank.enums.Rank;
import dev.plex.util.BungeeUtil;
import dev.plex.util.PlexUtils;
import dev.plex.util.TimeUtils;
import dev.plex.util.WebUtils;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;

@CommandParameters(name = "kick", description = "Kicks a player", usage = "/<command> <player>")
@CommandPermissions(level = Rank.ADMIN, permission = "plex.kick", source = RequiredCommandSource.ANY)
public class KickCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length == 0)
        {
            return usage();
        }

        PlexPlayer plexPlayer = DataUtils.getPlayer(args[0]);
        String reason = "No reason provided";

        if (plexPlayer == null)
        {
            throw new PlayerNotFoundException();
        }
        Player player = Bukkit.getPlayer(plexPlayer.getUuid());

        if (player == null)
        {
            throw new PlayerNotFoundException();
        }
        Punishment punishment = new Punishment(plexPlayer.getUuid(), getUUID(sender));
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
        PlexUtils.broadcast(messageComponent("kickedPlayer", sender.getName(), plexPlayer.getName()));
        BungeeUtil.kickPlayer(player, Punishment.generateKickMessage(punishment));
        return null;
    }
}