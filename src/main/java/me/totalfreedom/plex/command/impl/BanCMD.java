package me.totalfreedom.plex.command.impl;

import com.google.common.collect.ImmutableList;
import me.totalfreedom.plex.Plex;
import me.totalfreedom.plex.banning.Ban;
import me.totalfreedom.plex.cache.DataUtils;
import me.totalfreedom.plex.cache.PlayerCache;
import me.totalfreedom.plex.command.PlexCommand;
import me.totalfreedom.plex.command.annotation.CommandParameters;
import me.totalfreedom.plex.command.annotation.CommandPermissions;
import me.totalfreedom.plex.command.exception.PlayerNotFoundException;
import me.totalfreedom.plex.command.source.CommandSource;
import me.totalfreedom.plex.command.source.RequiredCommandSource;
import me.totalfreedom.plex.player.PlexPlayer;
import me.totalfreedom.plex.player.PunishedPlayer;
import me.totalfreedom.plex.punishment.Punishment;
import me.totalfreedom.plex.punishment.PunishmentType;
import me.totalfreedom.plex.rank.enums.Rank;
import me.totalfreedom.plex.util.PlexUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@CommandParameters(usage = "/<command> <player> [reason]", aliases = "offlineban,gtfo", description = "Bans a player, offline or online")
@CommandPermissions(level = Rank.ADMIN, source = RequiredCommandSource.ANY)

public class BanCMD extends PlexCommand
{
    public BanCMD() {
        super("ban");
    }

    @Override
    public void execute(CommandSource sender, String[] args)
    {
        if (args.length == 0)
        {
            sender.send(usage(getUsage()));
            return;
        }

        if (args.length == 1)
        {
            UUID targetUUID = PlexUtils.getFromName(args[0]);

            if (targetUUID == null || !DataUtils.hasPlayedBefore(targetUUID))
            {
                throw new PlayerNotFoundException();
            }
            PlexPlayer plexPlayer = DataUtils.getPlayer(targetUUID);

            if (isAdmin(plexPlayer))
            {
                if (!sender.isConsoleSender())
                {
                    PlexPlayer plexPlayer1 = sender.getPlexPlayer();
                    if (!plexPlayer1.getRankFromString().isAtLeast(plexPlayer.getRankFromString()))
                    {
                        sender.send("This player is an admin and a higher rank than you.");
                        return;
                    }
                }
            }

            PunishedPlayer punishedPlayer = PlayerCache.getPunishedPlayer(targetUUID) == null ? new PunishedPlayer(targetUUID) : PlayerCache.getPunishedPlayer(targetUUID);
            Punishment punishment = new Punishment(targetUUID, !sender.isConsoleSender() ? sender.getPlayer().getUniqueId() : null);
            punishment.setType(PunishmentType.BAN);
            punishment.setReason("");
            punishment.setPunishedUsername(plexPlayer.getName());
            punishment.setEndDate(new Date(Instant.now().plusSeconds(10/*PlexUtils.secondsToHours(24)*/).getEpochSecond()));
            punishment.setCustomTime(false);
            plugin.getPunishmentManager().doPunishment(punishedPlayer, punishment);
            Bukkit.broadcastMessage(sender.getName() + " - Banning " + plexPlayer.getName());
            if (Bukkit.getPlayer(targetUUID) != null)
            {
                Bukkit.getPlayer(targetUUID).kickPlayer("§cYou've been banned.");
            }
        }
    }

    @Override
    public List<String> onTabComplete(CommandSource sender, String[] args) {
        return args.length == 1 ? PlexUtils.getPlayerNameList() : ImmutableList.of();
    }
}
