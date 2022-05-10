package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.Plex;
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
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import dev.plex.util.TimeUtils;
import dev.plex.util.WebUtils;
import java.time.ZoneId;
import java.time.ZonedDateTime;
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

        UUID targetUUID = WebUtils.getFromName(args[0]);

        if (targetUUID == null || !DataUtils.hasPlayedBefore(targetUUID))
        {
            throw new PlayerNotFoundException();
        }
        PlexPlayer plexPlayer = DataUtils.getPlayer(targetUUID);
        Player player = Bukkit.getPlayer(targetUUID);

        if (plugin.getSystem().equalsIgnoreCase("ranks"))
        {
            if (isAdmin(plexPlayer))
            {
                if (!isConsole(sender))
                {
                    assert playerSender != null;
                    PlexPlayer plexPlayer1 = getPlexPlayer(playerSender);
                    if (!plexPlayer1.getRankFromString().isAtLeast(plexPlayer.getRankFromString()))
                    {
                        return messageComponent("higherRankThanYou");
                    }
                }
            }
        }

        plugin.getPunishmentManager().isAsyncBanned(targetUUID).whenComplete((aBoolean, throwable) ->
        {
            if (aBoolean)
            {
                send(sender, messageComponent("playerBanned"));
                return;
            }
            String reason;
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
            ZonedDateTime date = ZonedDateTime.now(ZoneId.of(TimeUtils.TIMEZONE));
            punishment.setEndDate(date.plusDays(1));
            punishment.setCustomTime(false);
            punishment.setActive(!isAdmin(plexPlayer));
            if (player != null)
            {
                punishment.setIp(player.getAddress().getAddress().getHostAddress().trim());
            }
            plugin.getPunishmentManager().punish(plexPlayer, punishment);
            PlexUtils.broadcast(messageComponent("banningPlayer", sender.getName(), plexPlayer.getName()));
            Bukkit.getScheduler().runTask(Plex.get(), () ->
            {
                if (player != null)
                {
                    BungeeUtil.kickPlayer(player, Punishment.generateBanMessage(punishment));
                }
            });
            PlexLog.debug("(From /ban command) PunishedPlayer UUID: " + plexPlayer.getUuid());
        });

        return null;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        return args.length == 1 && checkTab(sender, Rank.ADMIN, "plex.ban") ? PlexUtils.getPlayerNameList() : ImmutableList.of();
    }
}
