package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
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
import net.kyori.adventure.text.Component;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

@CommandParameters(name = "tempban", usage = "/<command> <player> <time> [reason]", description = "Temporarily ban a player")
@CommandPermissions(permission = "plex.tempban", source = RequiredCommandSource.ANY)

public class TempbanCMD extends PlexCommand
{
    @Override
    public Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length <= 1)
        {
            return usage();
        }

        PlexPlayer target = DataUtils.getPlayer(args[0]);
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
        boolean rollBack = true;
        if (args.length > 2)
        {
            reason = StringUtils.join(args, " ", 2, args.length);
            String newReason = StringUtils.normalizeSpace(reason.replace("-nrb", ""));
            punishment.setReason(newReason.trim().isEmpty() ? "No reason provided." : newReason);
            rollBack = !reason.startsWith("-nrb") && !reason.endsWith("-nrb");
        }
        else
        {
            punishment.setReason("No reason provided.");
        }
        punishment.setPunishedUsername(target.getName());
        punishment.setEndDate(TimeUtils.createDate(args[1]));
        punishment.setCustomTime(false);
        punishment.setActive(true);
        if (player != null)
        {
            punishment.setIp(player.getAddress().getAddress().getHostAddress().trim());
        }
        plugin.getPunishmentManager().punish(target, punishment);
        PlexUtils.broadcast(messageComponent("banningPlayer", sender.getName(), target.getName()));
        if (player != null)
        {
            BungeeUtil.kickPlayer(player, Punishment.generateBanMessage(punishment));
        }
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
                    plugin.getCoreProtectHook().coreProtectAPI().performRollback(86400, Collections.singletonList(target.getName()), null, null, null, null, 0, null);
                });
            }
        }
        return null;
    }

    @Override
    public @NotNull List<String> smartTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        return args.length == 1 && silentCheckPermission(sender, this.getPermission()) ? PlexUtils.getPlayerNameList() : ImmutableList.of();
    }
}
