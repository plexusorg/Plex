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
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandParameters(name = "ban", usage = "/<command> <player> [reason]", aliases = "offlineban,gtfo", description = "Bans a player, offline or online")
@CommandPermissions(level = Rank.ADMIN, permission = "plex.ban", source = RequiredCommandSource.ANY)

public class BanCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length == 0)
        {
            return usage();
        }

        UUID targetUUID = PlexUtils.getFromName(args[0]);
        String reason;

        if (targetUUID == null || !DataUtils.hasPlayedBefore(targetUUID))
        {
            throw new PlayerNotFoundException();
        }
        PlexPlayer plexPlayer = DataUtils.getPlayer(targetUUID);
        Player player = Bukkit.getPlayer(targetUUID);

        if (isAdmin(plexPlayer))
        {
            if (!isConsole(sender))
            {
                PlexPlayer plexPlayer1 = getPlexPlayer(playerSender);
                if (!plexPlayer1.getRankFromString().isAtLeast(plexPlayer.getRankFromString()))
                {
                    return tl("higherRankThanYou");
                }
            }
        }

        PunishedPlayer punishedPlayer = PlayerCache.getPunishedPlayer(targetUUID) == null ? new PunishedPlayer(targetUUID) : PlayerCache.getPunishedPlayer(targetUUID);
        Punishment punishment = new Punishment(targetUUID, getUUID(sender));
        punishment.setType(PunishmentType.BAN);
        if (args.length > 1)
        {
            reason = StringUtils.join(args, " ", 1, args.length);
            punishment.setReason(reason);
        }
        else
        {
            punishment.setReason("No reason provided.");
        }
        punishment.setPunishedUsername(plexPlayer.getName());
        LocalDateTime date = LocalDateTime.now();
        punishment.setEndDate(date.plusDays(1));
        punishment.setCustomTime(false);
        plugin.getPunishmentManager().doPunishment(punishedPlayer, punishment);
        PlexUtils.broadcast(tl("banningPlayer", sender.getName(), plexPlayer.getName()));
        if (player != null)
        {
            player.kick(componentFromString("&cYou've been banned."));
        }
        PlexLog.debug("(From /ban command) PunishedPlayer UUID: " + punishedPlayer.getUuid());
        return null;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        return args.length == 1 && isAdmin(sender) ? PlexUtils.getPlayerNameList() : ImmutableList.of();
    }
}
