package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.cache.PlayerCache;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.player.PlexPlayer;
import dev.plex.player.PunishedPlayer;
import dev.plex.punishment.Punishment;
import dev.plex.punishment.PunishmentType;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandParameters(name = "mute", description = "Mute a player on the server", usage = "/<command> <player>", aliases = "stfu")
@CommandPermissions(level = Rank.ADMIN, permission = "plex.mute")
public class MuteCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length != 1)
        {
            return usage();
        }
        Player player = getNonNullPlayer(args[0]);
        PunishedPlayer punishedPlayer = PlayerCache.getPunishedPlayer(player.getUniqueId());

        if (punishedPlayer.isMuted())
        {
            return messageComponent("playerMuted");
        }

        if (isAdmin(getPlexPlayer(player)))
        {
            if (!isConsole(sender))
            {
                assert playerSender != null;
                PlexPlayer plexPlayer1 = getPlexPlayer(playerSender);
                if (!plexPlayer1.getRankFromString().isAtLeast(getPlexPlayer(player).getRankFromString()))
                {
                    return messageComponent("higherRankThanYou");
                }
            }
        }

        Punishment punishment = new Punishment(UUID.fromString(punishedPlayer.getUuid()), getUUID(sender));
        punishment.setCustomTime(false);
        LocalDateTime date = LocalDateTime.now();
        punishment.setEndDate(date.plusMinutes(5));
        punishment.setType(PunishmentType.MUTE);
        punishment.setPunishedUsername(player.getName());
        punishment.setReason("");

        plugin.getPunishmentManager().doPunishment(punishedPlayer, punishment);
        PlexUtils.broadcast(messageComponent("mutedPlayer", sender.getName(), player.getName()));
        return null;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        return args.length == 1 && checkTab(sender, Rank.ADMIN, "plex.mute") ? PlexUtils.getPlayerNameList() : ImmutableList.of();
    }
}