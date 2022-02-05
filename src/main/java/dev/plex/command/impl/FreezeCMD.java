package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.cache.PlayerCache;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.CommandArgumentException;
import dev.plex.player.PunishedPlayer;
import dev.plex.punishment.Punishment;
import dev.plex.punishment.PunishmentType;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang.time.DateUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandParameters(name = "freeze", description = "Freeze a player on the server", usage = "/<command> <player>")
@CommandPermissions(level = Rank.ADMIN, permission = "plex.freeze")
public class FreezeCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length != 1)
        {
            throw new CommandArgumentException();
        }
        Player player = getNonNullPlayer(args[0]);
        PunishedPlayer punishedPlayer = PlayerCache.getPunishedPlayer(player.getUniqueId());
        Punishment punishment = new Punishment(UUID.fromString(punishedPlayer.getUuid()), getUUID(sender));
        punishment.setCustomTime(false);
        LocalDateTime date = LocalDateTime.now();
        punishment.setEndDate(date.plusMinutes(5));
        punishment.setType(PunishmentType.FREEZE);
        punishment.setPunishedUsername(player.getName());
        punishment.setReason("");

        plugin.getPunishmentManager().doPunishment(punishedPlayer, punishment);
        PlexUtils.broadcast(tl("frozePlayer", sender.getName(), player.getName()));
        return null;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        return args.length == 1 && isAdmin(sender) ? PlexUtils.getPlayerNameList() : ImmutableList.of();
    }
}