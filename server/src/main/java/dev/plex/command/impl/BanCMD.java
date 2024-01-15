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
import dev.plex.util.*;
//import me.botsko.prism.api.PrismParameters;
//import me.botsko.prism.api.Result;
//import me.botsko.prism.api.actions.PrismProcessType;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@CommandParameters(name = "ban", usage = "/<command> <player> [-nrb] [reason] [-nrb]", aliases = "offlineban,gtfo", description = "Bans a player, offline or online")
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
            boolean rollBack = true;
            if (args.length > 1)
            {
                reason = StringUtils.join(args, " ", 1, args.length);
                String newReason = StringUtils.normalizeSpace(reason.replace("-nrb", ""));
                punishment.setReason(newReason.trim().isEmpty() ? "No reason provided." : newReason);
                rollBack = !reason.startsWith("-nrb") && !reason.endsWith("-nrb");
            }
            else
            {
                punishment.setReason("No reason provided.");
            }
            punishment.setPunishedUsername(plexPlayer.getName());
            ZonedDateTime date = ZonedDateTime.now(ZoneId.of(TimeUtils.TIMEZONE));
            punishment.setEndDate(date.plusDays(1));
            punishment.setCustomTime(false);
            punishment.setActive(true);
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

            if (rollBack)
            {
                /*if (plugin.getPrismHook().hasPrism()) {
                    PrismParameters parameters = plugin.getPrismHook().prismApi().createParameters();
                    parameters.addActionType("block-place");
                    parameters.addActionType("block-break");
                    parameters.addActionType("block-burn");
                    parameters.addActionType("entity-spawn");
                    parameters.addActionType("entity-kill");
                    parameters.addActionType("entity-explode");
                    parameters.addPlayerName(plexPlayer.getName());
                    parameters.setBeforeTime(Instant.now().toEpochMilli());
                    parameters.setProcessType(PrismProcessType.ROLLBACK);
                    final Future<Result> result = plugin.getPrismHook().prismApi().performLookup(parameters, sender);
                    Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> {
                        try
                        {
                            final Result done = result.get();
                        } catch (InterruptedException | ExecutionException e)
                        {
                            throw new RuntimeException(e);
                        }
                    });
                }
                else */
                if (plugin.getCoreProtectHook().hasCoreProtect())
                {
                    PlexLog.debug("Testing coreprotect");
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
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        return args.length == 1 && silentCheckPermission(sender, "plex.ban") ? PlexUtils.getPlayerNameList() : ImmutableList.of();
    }
}
