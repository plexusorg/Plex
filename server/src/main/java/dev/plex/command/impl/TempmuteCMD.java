package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.Punishment;
import dev.plex.punishment.PunishmentType;
import dev.plex.util.PlexUtils;
import dev.plex.util.TimeUtils; // Import your TimeUtils
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;

@CommandParameters(name = "tempmute", description = "Temporarily mute a player on the server",
        usage = "/<command> <player> <time> [reason]", aliases = "tmute")
@CommandPermissions(permission = "plex.tempmute")
public class TempmuteCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length < 2)
        {
            return usage();
        }

        Player player = getNonNullPlayer(args[0]);
        PlexPlayer punishedPlayer = getOfflinePlexPlayer(player.getUniqueId());

        if (punishedPlayer.isMuted())
        {
            return messageComponent("playerMuted");
        }

        if (silentCheckPermission(player, "plex.tempmute"))
        {
            send(sender, messageComponent("higherRankThanYou"));
            return null;
        }

        ZonedDateTime endDate;
        try
        {
            endDate = TimeUtils.createDate(args[1]);
        }
        catch (NumberFormatException e)
        {
            return messageComponent("invalidTimeFormat");
        }

        if (endDate.isBefore(ZonedDateTime.now()))
        {
            return messageComponent("timeMustBeFuture");
        }

        ZonedDateTime oneWeekFromNow = ZonedDateTime.now().plusWeeks(1);
        if (endDate.isAfter(oneWeekFromNow))
        {
            return messageComponent("maxTimeExceeded");
        }

        final String reason = args.length >= 3 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length))
                : messageString("noReasonProvided");

        Punishment punishment = new Punishment(punishedPlayer.getUuid(), getUUID(sender));
        punishment.setCustomTime(true);
        punishment.setEndDate(endDate);
        punishment.setType(PunishmentType.MUTE);
        punishment.setPunishedUsername(player.getName());
        punishment.setIp(player.getAddress().getAddress().getHostAddress().trim());
        punishment.setReason(reason);
        punishment.setActive(true);

        plugin.getPunishmentManager().punish(punishedPlayer, punishment);
        PlexUtils.broadcast(messageComponent("tempMutedPlayer", sender.getName(), player.getName(), TimeUtils.formatRelativeTime(endDate)));
        return null;
    }

    @Override
    public @NotNull List<String> smartTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        return args.length == 1 && silentCheckPermission(sender, this.getPermission()) ? PlexUtils.getPlayerNameList() : ImmutableList.of();
    }
}
