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
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@CommandParameters(name = "ban", usage = "/<command> <player> [reason]", aliases = "offlineban,gtfo", description = "Bans a player, offline or online")
@CommandPermissions(level = Rank.ADMIN, permission = "plex.ban", source = RequiredCommandSource.ANY)

public class BanCMD extends PlexCommand
{
    @Override
    public Component execute(CommandSender sender, Player playerSender, String[] args)
    {
        if (args.length == 0)
        {
            return usage(getUsage());
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
                if (!isConsole(sender))
                {
                    PlexPlayer plexPlayer1 = getPlexPlayer((Player)sender);
                    if (!plexPlayer1.getRankFromString().isAtLeast(plexPlayer.getRankFromString()))
                    {
                        return tl("higherRankThanYou");
                    }
                }
            }

            PunishedPlayer punishedPlayer = PlayerCache.getPunishedPlayer(targetUUID) == null ? new PunishedPlayer(targetUUID) : PlayerCache.getPunishedPlayer(targetUUID);
            Punishment punishment = new Punishment(targetUUID, getUUID(sender));
            punishment.setType(PunishmentType.BAN);
            punishment.setReason("");
            punishment.setPunishedUsername(plexPlayer.getName());
            punishment.setEndDate(new Date(Instant.now().plusSeconds(PlexUtils.hoursToSeconds(24)).getEpochSecond()));
            punishment.setCustomTime(false);
            plugin.getPunishmentManager().doPunishment(punishedPlayer, punishment);
            PlexUtils.broadcast(tl("banningPlayer", sender.getName(), plexPlayer.getName()));
            if (Bukkit.getPlayer(targetUUID) != null)
            {
                Bukkit.getPlayer(targetUUID).kick(componentFromString("&cYou've been banned."));
            }
        }
        return null;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        return args.length == 1 && isAdmin(sender) ? PlexUtils.getPlayerNameList() : ImmutableList.of();
    }
}
