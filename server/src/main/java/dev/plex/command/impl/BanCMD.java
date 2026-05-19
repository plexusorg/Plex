package dev.plex.command.impl;


import dev.plex.command.ServerCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.PlayerNotFoundException;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.Punishment;
import dev.plex.punishment.PunishmentType;
import dev.plex.util.BungeeUtil;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import dev.plex.util.TimeUtils;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;

import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandParameters(name = "ban", usage = "/<command> <player> [reason] [-rb]", aliases = "offlineban,gtfo", description = "Bans a player, offline or online")
@CommandPermissions(permission = "plex.ban", source = RequiredCommandSource.ANY)

public class BanCMD extends ServerCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length == 0)
        {
            return usage();
        }

        final PlexPlayer plexPlayer = plugin.getPlayerService().getPlayer(args[0]);

        if (plexPlayer == null)
        {
            throw new PlayerNotFoundException();
        }

        Player player = Bukkit.getPlayer(plexPlayer.getUuid());

        plugin.getPunishmentManager().isAsyncBanned(plexPlayer.getUuid()).whenComplete((aBoolean, throwable) ->
        {
            plugin.getApi().scheduler().runGlobal(() ->
            {
                if (throwable != null)
                {
                    PlexLog.error("Unable to check ban state for {0}: {1}", plexPlayer.getName(), throwable.getMessage());
                    return;
                }
                if (aBoolean)
                {
                    send(sender, messageComponent("playerBanned"));
                    return;
                }
                String reason;
                Punishment punishment = new Punishment(plexPlayer.getUuid(), getUUID(sender));
                punishment.setType(PunishmentType.BAN);
                boolean rollBack = false;
                if (args.length > 1)
                {
                    reason = StringUtils.join(args, " ", 1, args.length);
                    String newReason = StringUtils.normalizeSpace(reason.replace("-rb", ""));
                    punishment.setReason(newReason.trim().isEmpty() ? messageString("noReasonProvided") : newReason);
                    rollBack = reason.startsWith("-rb") || reason.endsWith("-rb");
                }
                else
                {
                    punishment.setReason(messageString("noReasonProvided"));
                }
                punishment.setPunishedUsername(plexPlayer.getName());
                ZonedDateTime date = ZonedDateTime.now(ZoneId.of(TimeUtils.TIMEZONE));
                punishment.setEndDate(date.plusDays(1));
                punishment.setCustomTime(false);
                punishment.setActive(true);
                punishment.setIp(plexPlayer.getIps().getLast());
                plugin.getPunishmentManager().punish(plexPlayer, punishment);
                PlexUtils.broadcast(messageComponent("banningPlayer", sender.getName(), plexPlayer.getName()));
                if (player != null)
                {
                    plugin.getApi().scheduler().runEntity(player, () -> BungeeUtil.kickPlayer(plugin, player, Punishment.generateBanMessage(punishment, plugin.config.getString("banning.ban_url"), plugin.getPlayerService())));
                }
                PlexLog.debug("(From /ban command) PunishedPlayer UUID: " + plexPlayer.getUuid());

                if (rollBack)
                {
                    plugin.getApi().rollback().rollbackLastDay(sender, plexPlayer.getName());
                }
            });
        });

        return null;
    }

    @Override
    public @NotNull List<String> smartTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        if (args.length == 1 && silentCheckPermission(sender, this.getPermission()))
        {
            return PlexUtils.getPlayerNameList();
        }
        if (args.length > 1 && silentCheckPermission(sender, this.getPermission()))
        {
            return Collections.singletonList("-rb");
        }
        return Collections.emptyList();
    }
}
