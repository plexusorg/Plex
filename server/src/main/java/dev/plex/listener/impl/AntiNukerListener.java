package dev.plex.listener.impl;

import dev.plex.Plex;
import dev.plex.abuse.AbuseTracker;
import dev.plex.api.listener.EventRule;
import dev.plex.listener.ServerListenerBase;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.Punishment;
import dev.plex.api.punishment.PunishmentType;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import dev.plex.util.TimeUtils;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class AntiNukerListener extends ServerListenerBase
{
    private static final AbuseTracker TRACKER = new AbuseTracker(
            Duration.ofSeconds(5), 200, 2, Duration.ofMinutes(30), 10_000);

    public AntiNukerListener(Plex plugin)
    {
        super(plugin);
        plugin.getApi().listeners().register(this,
                EventRule.of(BlockPlaceEvent.class, EventPriority.HIGH, event -> checkForNuker(event.getPlayer(), event)),
                EventRule.of(BlockBreakEvent.class, EventPriority.HIGH, event -> checkForNuker(event.getPlayer(), event)));
    }

    private void checkForNuker(Player player, Cancellable event)
    {
        UUID uuid = player.getUniqueId();
        AbuseTracker.Decision decision = TRACKER.record(uuid);
        if (!decision.allowed())
        {
            event.setCancelled(true);
            CompletableFuture<Void> ban = decision.escalationTriggered() ? issueBan(player) : null;
            if (decision.thresholdCrossed() && !decision.escalationTriggered())
            {
                player.kick(PlexUtils.messageComponent("nukerKickMessage"));
            }
            if (ban != null)
            {
                ban.whenComplete((ignored, throwable) ->
                {
                    if (throwable != null)
                    {
                        TRACKER.reset(uuid);
                        PlexLog.error("Unable to complete automatic nuker tempban for {0}: {1}", uuid, throwable.getMessage());
                    }
                });
            }
        }
    }

    private CompletableFuture<Void> issueBan(Player player)
    {
        UUID uuid = player.getUniqueId();
        PlexPlayer plexPlayer = plugin.getPlayerService().cachedPlayer(uuid);
        if (plexPlayer == null)
        {
            return CompletableFuture.failedFuture(new IllegalStateException("player session is not loaded"));
        }

        Punishment punishment = new Punishment(uuid, null);
        punishment.setType(PunishmentType.TEMPBAN);
        punishment.setReason(PlexUtils.messageString("nukerTempbanReason"));
        if (player.getAddress() != null)
        {
            punishment.setIp(player.getAddress().getAddress().getHostAddress());
        }
        punishment.setEndDate(TimeUtils.createDate("5m"));
        return plugin.getPunishmentManager().punish(plexPlayer, punishment);
    }
}
