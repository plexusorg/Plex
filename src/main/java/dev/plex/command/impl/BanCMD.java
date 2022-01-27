package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.cache.DataUtils;
import dev.plex.cache.PlayerCache;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.PlayerNotFoundException;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.player.PlexPlayer;
import dev.plex.player.PunishedPlayer;
import dev.plex.punishment.Punishment;
import dev.plex.punishment.PunishmentType;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import org.bukkit.Bukkit;

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
    public Component execute(CommandSender sender, String[] args)
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
                        sender.send(tl("higherRankThanYou"));
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
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return args.length == 1 ? PlexUtils.getPlayerNameList() : ImmutableList.of();
    }
}
