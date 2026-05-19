package dev.plex.command.impl;


import com.google.common.collect.ImmutableList;
import dev.plex.command.ServerCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.PlayerNotFoundException;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.Punishment;
import dev.plex.punishment.PunishmentType;
import dev.plex.util.BungeeUtil;
import dev.plex.util.PlexUtils;
import dev.plex.util.TimeUtils;

import java.util.List;

import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandParameters(name = "tempban", usage = "/<command> <player> <time> [reason] [-rb]", description = "Temporarily ban a player")
@CommandPermissions(permission = "plex.tempban", source = RequiredCommandSource.ANY)

public class TempbanCMD extends ServerCommand
{
    @Override
    public Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length <= 1)
        {
            return usage();
        }

        PlexPlayer target = plugin.getPlayerService().getPlayer(args[0]);
        String reason;

        if (target == null)
        {
            throw new PlayerNotFoundException();
        }
        Player player = Bukkit.getPlayer(target.getUuid());

        if (plugin.getPunishmentManager().isBanned(target.getUuid()))
        {
            return messageComponent("playerBanned");
        }
        Punishment punishment = new Punishment(target.getUuid(), getUUID(sender));
        punishment.setType(PunishmentType.TEMPBAN);
        boolean rollBack = false;
        if (args.length > 2)
        {
            reason = StringUtils.join(args, " ", 2, args.length);
            String newReason = StringUtils.normalizeSpace(reason.replace("-nrb", ""));
            punishment.setReason(newReason.trim().isEmpty() ? messageString("noReasonProvided") : newReason);
            rollBack = reason.startsWith("-rb") || reason.endsWith("-rb");
        }
        else
        {
            punishment.setReason(messageString("noReasonProvided"));
        }
        punishment.setPunishedUsername(target.getName());
        punishment.setEndDate(TimeUtils.createDate(args[1]));
        punishment.setCustomTime(false);
        punishment.setActive(true);
        punishment.setIp(target.getIps().getLast());
        plugin.getPunishmentManager().punish(target, punishment);
        PlexUtils.broadcast(messageComponent("banningPlayer", sender.getName(), target.getName()));
        if (player != null)
        {
            plugin.getApi().scheduler().runEntity(player, () -> BungeeUtil.kickPlayer(plugin, player, Punishment.generateBanMessage(punishment, plugin.config.getString("banning.ban_url"), plugin.getPlayerService())));
        }
        if (rollBack)
        {
            plugin.getApi().rollback().rollbackLastDay(sender, target.getName());
        }
        return null;
    }

    @Override
    public @NotNull List<String> smartTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        return args.length == 1 && silentCheckPermission(sender, this.getPermission()) ? PlexUtils.getPlayerNameList() : ImmutableList.of();
    }
}
