package me.totalfreedom.plex.command.impl;

import com.google.common.collect.ImmutableList;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import me.totalfreedom.plex.cache.PlayerCache;
import me.totalfreedom.plex.command.PlexCommand;
import me.totalfreedom.plex.command.annotation.CommandParameters;
import me.totalfreedom.plex.command.annotation.CommandPermissions;
import me.totalfreedom.plex.command.exception.CommandArgumentException;
import me.totalfreedom.plex.command.source.CommandSource;
import me.totalfreedom.plex.player.PunishedPlayer;
import me.totalfreedom.plex.punishment.Punishment;
import me.totalfreedom.plex.punishment.PunishmentType;
import me.totalfreedom.plex.rank.enums.Rank;
import me.totalfreedom.plex.util.PlexUtils;
import org.bukkit.entity.Player;

@CommandParameters(description = "Freeze a player on the server", usage = "/<command> <player>")
@CommandPermissions(level = Rank.ADMIN)
public class FreezeCMD extends PlexCommand
{
    public FreezeCMD()
    {
        super("freeze");
    }

    @Override
    public void execute(CommandSource sender, String[] args)
    {
        if (args.length != 1)
        {
            throw new CommandArgumentException();
        }
        Player player = getNonNullPlayer(args[0]);
        PunishedPlayer punishedPlayer = PlayerCache.getPunishedPlayer(player.getUniqueId());
        Punishment punishment = new Punishment(UUID.fromString(punishedPlayer.getUuid()), sender.isConsoleSender() ? null : sender.getPlayer().getUniqueId());
        punishment.setCustomTime(false);
        punishment.setEndDate(new Date(Instant.now().plusSeconds(10).toEpochMilli()));
        punishment.setType(PunishmentType.FREEZE);
        punishment.setPunishedUsername(player.getName());
        punishment.setReason("");

        plugin.getPunishmentManager().doPunishment(punishedPlayer, punishment);
        PlexUtils.broadcast(tl("frozePlayer", sender.getName(), player.getName()));
    }

    @Override
    public List<String> onTabComplete(CommandSource sender, String[] args)
    {
        return args.length == 1 ? PlexUtils.getPlayerNameList() : ImmutableList.of();
    }
}