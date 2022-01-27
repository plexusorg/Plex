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
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@CommandParameters(name = "freeze", description = "Freeze a player on the server", usage = "/<command> <player>")
@CommandPermissions(level = Rank.ADMIN)
public class FreezeCMD extends PlexCommand
{

    @Override
    public Component execute(CommandSender sender, String[] args)
    {
        if (args.length != 1)
        {
            throw new CommandArgumentException();
        }
        Player player = getNonNullPlayer(args[0]);
        PunishedPlayer punishedPlayer = PlayerCache.getPunishedPlayer(player.getUniqueId());
        Punishment punishment = new Punishment(UUID.fromString(punishedPlayer.getUuid()), getUUID(sender));
        punishment.setCustomTime(false);
        punishment.setEndDate(new Date(Instant.now().plusSeconds(10).toEpochMilli()));
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