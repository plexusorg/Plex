package dev.plex.command.impl;

import dev.plex.cache.DataUtils;
import dev.plex.command.PlexCommand;
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

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.prism_mc.prism.api.activities.ActivityQuery;
import org.prism_mc.prism.paper.api.PrismPaperApi;
import org.prism_mc.prism.paper.api.activities.PaperActivityQuery;

@CommandParameters(name = "ban", usage = "/<command> <player> [reason] [-rb]", aliases = "offlineban,gtfo", description = "Bans a player, offline or online")
@CommandPermissions(permission = "plex.ban", source = RequiredCommandSource.ANY)

public class BanCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length == 0)
        {
            return usage();
        }

        final PlexPlayer plexPlayer = DataUtils.getPlayer(args[0]);

        if (plexPlayer == null)
        {
            throw new PlayerNotFoundException();
        }

        Player player = Bukkit.getPlayer(plexPlayer.getUuid());

        plugin.getPunishmentManager().isAsyncBanned(plexPlayer.getUuid()).whenComplete((aBoolean, throwable) ->
        {
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
            punishment.setIp(player != null ? player.getAddress().getAddress().getHostAddress().trim() : plexPlayer.getIps().getLast());
            plugin.getPunishmentManager().punish(plexPlayer, punishment);
            PlexUtils.broadcast(messageComponent("banningPlayer", sender.getName(), plexPlayer.getName()));
            Bukkit.getGlobalRegionScheduler().execute(plugin, () ->
            {
                if (player != null)
                {
                    BungeeUtil.kickPlayer(player, Punishment.generateBanMessage(punishment));
                }
            });
            PlexLog.debug("(From /ban command) PunishedPlayer UUID: " + plexPlayer.getUuid());

            if (rollBack)
            {
                if (plugin.getPrismHook() != null && plugin.getPrismHook().hasPrism())
                {
                    PrismPaperApi prism = plugin.getPrismHook().getPrism();
                    ActivityQuery query = PaperActivityQuery.builder()
                            .actionTypeKeys(Arrays.asList("block-place", "block-break", "block-burn", "entity-spawn", "entity-kill", "entity-explode"))
                            .causePlayerName(plexPlayer.getName())
                            .before(Instant.now().getEpochSecond())
                            .after(Instant.now().getEpochSecond() - 86400)
                            .rollback()
                            .build();
                    prism.rollback(sender, query).whenCompleteAsync((result, error) ->
                    {
                        if (error != null)
                        {
                            send(sender, messageComponent("prismRollbackError", error.getMessage()));
                            PlexLog.error("Unable to rollback: {0}", error);
                            return;
                        }

                        int count = result.applied();
                        if (count == 0)
                        {
                            send(sender, messageComponent("prismNoResult", count));
                            PlexLog.debug("No activities are available to rollback");
                            return;
                        }

                        send(sender, messageComponent("prismRollbackMessage", count));
                        PlexLog.debug("Rolled back {0} activities", count);
                    });
                }
                else if (plugin.getCoreProtectHook() != null && plugin.getCoreProtectHook().hasCoreProtect())
                {
                    Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask ->
                    {
                        plugin.getCoreProtectHook().coreProtectAPI().performRollback(86400, Collections.singletonList(plexPlayer.getName()), null, null, null, null, 0, null);
                    });
                }
            }
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
