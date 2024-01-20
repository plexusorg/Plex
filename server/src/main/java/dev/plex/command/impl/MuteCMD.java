package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.Punishment;
import dev.plex.punishment.PunishmentType;
import dev.plex.util.PlexUtils;
import dev.plex.util.TimeUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@CommandParameters(name = "mute", description = "Mute a player on the server", usage = "/<command> <player>", aliases = "stfu")
@CommandPermissions(permission = "plex.mute")
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
        PlexPlayer punishedPlayer = getOfflinePlexPlayer(player.getUniqueId());

        if (punishedPlayer.isMuted())
        {
            return messageComponent("playerMuted");
        }

        if (silentCheckPermission(player, "plex.mute"))
        {
            send(sender, messageComponent("higherRankThanYou"));
            return null;
        }

        Punishment punishment = new Punishment(punishedPlayer.getUuid(), getUUID(sender));
        punishment.setCustomTime(false);
        ZonedDateTime date = ZonedDateTime.now(ZoneId.of(TimeUtils.TIMEZONE));
        punishment.setEndDate(date.plusMinutes(5));
        punishment.setType(PunishmentType.MUTE);
        punishment.setPunishedUsername(player.getName());
        punishment.setIp(player.getAddress().getAddress().getHostAddress().trim());
        punishment.setReason("");

        plugin.getPunishmentManager().punish(punishedPlayer, punishment);
        PlexUtils.broadcast(messageComponent("mutedPlayer", sender.getName(), player.getName()));
        return null;
    }

    @Override
    public @NotNull List<String> smartTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        return args.length == 1 && silentCheckPermission(sender, this.getPermission()) ? PlexUtils.getPlayerNameList() : ImmutableList.of();
    }
}