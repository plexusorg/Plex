package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
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

import java.util.List;
import java.util.UUID;

@CommandParameters(name = "tempban", usage = "/<command> <player> <time> [reason]", description = "Temporarily ban a player")
@CommandPermissions(level = Rank.ADMIN, permission = "plex.tempban", source = RequiredCommandSource.ANY)

public class TempbanCMD extends PlexCommand
{
    @Override
    public Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length <= 1)
        {
            return usage();
        }

        UUID targetUUID = WebUtils.getFromName(args[0]);
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
                assert playerSender != null;
                PlexPlayer plexPlayer1 = getPlexPlayer(playerSender);
                if (!plexPlayer1.getRankFromString().isAtLeast(plexPlayer.getRankFromString()) && plexPlayer.isAdminActive())
                {
                    return messageComponent("higherRankThanYou");
                }
            }
        }

        if (plugin.getPunishmentManager().isBanned(targetUUID))
        {
            return messageComponent("playerBanned");
        }
        Punishment punishment = new Punishment(targetUUID, getUUID(sender));
        punishment.setType(PunishmentType.TEMPBAN);
        if (args.length > 2)
        {
            reason = StringUtils.join(args, " ", 2, args.length);
            punishment.setReason(reason);
        }
        else
        {
            punishment.setReason("No reason provided.");
        }
        punishment.setPunishedUsername(plexPlayer.getName());
        punishment.setEndDate(TimeUtils.createDate(args[1]));
        punishment.setCustomTime(false);
        punishment.setActive(!isAdmin(plexPlayer));
        if (player != null)
        {
            punishment.setIp(player.getAddress().getAddress().getHostAddress().trim());
        }
        plugin.getPunishmentManager().punish(plexPlayer, punishment);
        PlexUtils.broadcast(messageComponent("banningPlayer", sender.getName(), plexPlayer.getName()));
        if (player != null)
        {
            BungeeUtil.kickPlayer(player, Punishment.generateBanMessage(punishment));
        }
        return null;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        return args.length == 1 && silentCheckRank(sender, Rank.ADMIN, "plex.tempban") ? PlexUtils.getPlayerNameList() : ImmutableList.of();
    }
}
