package dev.plex.punishment;

import dev.plex.Plex;
import dev.plex.api.punishment.PunishmentType;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.admission.BanDecisionService;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import dev.plex.util.BanKickUtil;
import dev.plex.util.TimeUtils;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class PunishmentManager
{
    private final Plex plugin;
    private final BanDecisionService banDecisionService;
    private volatile List<IndefiniteBan> indefiniteBans = List.of();
    private final ConcurrentHashMap<StateKey, ScheduledTask> timedTasks = new ConcurrentHashMap<>();

    public PunishmentManager(Plex plugin)
    {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        int cacheSize = Math.max(100, plugin.config.getInt("banning.admission-cache-size", 10_000));
        long ttlSeconds = Math.max(1, plugin.config.getLong("banning.admission-cache-seconds", 60));
        this.banDecisionService = new BanDecisionService(plugin.getPunishmentRepository(), Duration.ofSeconds(ttlSeconds), cacheSize);
    }

    public void mergeIndefiniteBans()
    {
        indefiniteBans = plugin.indefBans.getKeys(false).stream().map(key -> new IndefiniteBan(
                plugin.getIndefBans().getStringList(key + ".users"),
                plugin.getIndefBans().getStringList(key + ".uuids").stream().map(UUID::fromString).toList(),
                plugin.getIndefBans().getStringList(key + ".ips"),
                plugin.getIndefBans().getString(key + ".reason", ""))).toList();

        PlexLog.log("Loaded {0} UUID(s), {1} IP(s), and {2} username(s) as indefinitely banned",
                indefiniteBans.stream().map(IndefiniteBan::getUuids).mapToLong(Collection::size).sum(),
                indefiniteBans.stream().map(IndefiniteBan::getIps).mapToLong(Collection::size).sum(),
                indefiniteBans.stream().map(IndefiniteBan::getUsernames).mapToLong(Collection::size).sum());
    }

    public List<IndefiniteBan> getIndefiniteBans() { return indefiniteBans; }

    @Nullable
    public IndefiniteBan getIndefiniteBanByUUID(UUID uuid)
    {
        return indefiniteBans.stream().filter(ban -> ban.getUuids().contains(uuid)).findFirst().orElse(null);
    }

    @Nullable
    public IndefiniteBan getIndefiniteBanByIP(String ip)
    {
        String canonicalIp = BanDecisionService.canonicalIp(ip);
        return indefiniteBans.stream().filter(ban -> ban.getIps().stream()
                .map(BanDecisionService::canonicalIp).anyMatch(canonicalIp::equals)).findFirst().orElse(null);
    }

    @Nullable
    public IndefiniteBan getIndefiniteBanByUsername(String username)
    {
        return indefiniteBans.stream().filter(ban -> ban.getUsernames().stream()
                .anyMatch(name -> name.equalsIgnoreCase(username))).findFirst().orElse(null);
    }

    public synchronized boolean banUsername(String username, String reason)
    {
        if (getIndefiniteBanByUsername(username) != null)
        {
            return false;
        }
        String key = nextIndefiniteBanKey("name", username);
        plugin.indefBans.set(key + ".reason", reason);
        plugin.indefBans.set(key + ".users", List.of(username));
        plugin.indefBans.save();
        mergeIndefiniteBans();
        return true;
    }

    public synchronized boolean banIp(String ip, String reason)
    {
        String canonicalIp = BanDecisionService.canonicalIp(ip);
        if (getIndefiniteBanByIP(canonicalIp) != null)
        {
            return false;
        }
        String key = nextIndefiniteBanKey("ip", canonicalIp);
        plugin.indefBans.set(key + ".reason", reason);
        plugin.indefBans.set(key + ".ips", List.of(canonicalIp));
        plugin.indefBans.save();
        mergeIndefiniteBans();
        return true;
    }

    public CompletableFuture<Optional<Punishment>> decideAdmission(UUID uuid, @Nullable String ip)
    {
        if (hasBanBypass(uuid))
        {
            return CompletableFuture.completedFuture(Optional.empty());
        }
        return banDecisionService.decide(uuid, ip);
    }

    public void invalidateBanDecisions(UUID uuid, @Nullable String ip)
    {
        banDecisionService.invalidate(uuid, ip);
    }

    public boolean isActiveBan(Punishment punishment)
    {
        return punishment.getType().isBan() && isPunishmentActive(punishment);
    }

    public boolean isPunishmentActive(Punishment punishment)
    {
        if (!punishment.isActive()) return false;
        if (punishment.getEndDate() != null && !punishment.getEndDate().isAfter(ZonedDateTime.now(TimeUtils.zoneId())))
        {
            return false;
        }
        return !punishment.getType().isBan() || !hasBanBypass(punishment.getPunished());
    }

    public boolean hasActivePunishment(PlexPlayer player, PunishmentType type)
    {
        return player.getPunishments().stream()
                .anyMatch(punishment -> punishment.getType() == type && isPunishmentActive(punishment));
    }

    public CompletableFuture<Boolean> isBanned(UUID uuid)
    {
        return isBanned(uuid, null);
    }

    public CompletableFuture<Boolean> isBanned(UUID uuid, @Nullable String ip)
    {
        return decideAdmission(uuid, ip).thenApply(Optional::isPresent);
    }

    public CompletableFuture<List<Punishment>> getActiveBans()
    {
        return plugin.getPunishmentRepository().getPunishments().thenApply(punishments -> punishments.stream()
                .filter(this::isActiveBan).toList());
    }

    public CompletableFuture<Boolean> unban(UUID uuid)
    {
        return plugin.getPunishmentRepository().removeBan(uuid).thenApply(removal ->
        {
            if (!removal.changed()) return false;
            PlexPlayer player = plugin.getPlayerService().getCachedPlayer(uuid);
            if (player != null)
            {
                player.getPunishments().stream()
                        .filter(p -> p.getType().isBan())
                        .forEach(p -> p.setActive(false));
            }
            invalidateBanDecisions(uuid, null);
            if (removal.ips().isEmpty()) publishInvalidation(uuid, null);
            for (String ip : removal.ips())
            {
                invalidateBanDecisions(uuid, ip);
                publishInvalidation(uuid, ip);
            }
            return true;
        });
    }

    public CompletableFuture<Void> punish(PlexPlayer player, Punishment punishment)
    {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(punishment, "punishment");
        if (!player.getUuid().equals(punishment.getPunished()))
            return CompletableFuture.failedFuture(new IllegalArgumentException("Punishment and player UUIDs differ"));
        if (punishment.getType() == null)
            return CompletableFuture.failedFuture(new IllegalArgumentException("Punishment type is required"));
        if (punishment.getIssueDate() == null)
            return CompletableFuture.failedFuture(new IllegalArgumentException("Punishment issue date is required"));
        punishment.getType().fixedDuration().ifPresent(duration ->
                punishment.setEndDate(punishment.getIssueDate().plus(duration)));
        if (punishment.getType().requiresEndDate() && punishment.getEndDate() == null)
            return CompletableFuture.failedFuture(new IllegalArgumentException(punishment.getType() + " requires an end date"));
        if (!punishment.getType().requiresEndDate() && punishment.getEndDate() != null)
            return CompletableFuture.failedFuture(new IllegalArgumentException(punishment.getType() + " must not have an end date"));
        if (punishment.getEndDate() != null && !punishment.getEndDate().toInstant().isAfter(Instant.now()))
            return CompletableFuture.failedFuture(new IllegalArgumentException(punishment.getType() + " requires a future end date"));
        if (punishment.getEndDate() != null && punishment.getType().maximumDuration().isPresent()
                && punishment.getEndDate().toInstant().isAfter(punishment.getIssueDate().toInstant()
                .plus(punishment.getType().maximumDuration().orElseThrow())))
            return CompletableFuture.failedFuture(new IllegalArgumentException(punishment.getType() + " exceeds its maximum duration"));
        if (punishment.getReason() == null)
            return CompletableFuture.failedFuture(new IllegalArgumentException("Punishment reason is required"));
        if (punishment.getIp() != null) punishment.setIp(BanDecisionService.canonicalIp(punishment.getIp()));
        punishment.setActive(punishment.getType().startsActive());

        CompletableFuture<Boolean> alreadyActive = punishment.getType().isBan()
                ? isBanned(player.getUuid(), punishment.getIp())
                : CompletableFuture.completedFuture(false);
        return alreadyActive.thenCompose(active ->
        {
            if (active)
            {
                return CompletableFuture.failedFuture(new IllegalStateException("Player is already banned"));
            }
            return plugin.getPunishmentRepository().insertPunishment(punishment);
        }).thenRun(() ->
        {
            player.getPunishments().add(punishment);
            if (punishment.getType().isBan())
            {
                invalidateBanDecisions(player.getUuid(), punishment.getIp());
                publishInvalidation(player.getUuid(), punishment.getIp());
                if (isPunishmentActive(punishment))
                {
                    BanKickUtil.kickBannedPlayers(plugin, player.getUuid(), punishment.getIp(),
                            Punishment.generateBanMessage(punishment, plugin.config.getString("banning.ban_url")));
                }
            }
            restoreTimedState(player, punishment.getType());
        });
    }

    public void restoreTimedState(PlexPlayer player)
    {
        restoreTimedState(player, PunishmentType.MUTE);
        restoreTimedState(player, PunishmentType.FREEZE);
    }

    public CompletableFuture<Void> deactivateTimedPunishment(PlexPlayer player, PunishmentType type)
    {
        return plugin.getPunishmentRepository().updatePunishment(type, false, player.getUuid()).thenRun(() ->
        {
            player.getPunishments().stream().filter(p -> p.getType() == type).forEach(p -> p.setActive(false));
            restoreTimedState(player, type);
        });
    }

    private void restoreTimedState(PlexPlayer player, PunishmentType type)
    {
        if (type != PunishmentType.MUTE && type != PunishmentType.FREEZE) return;
        ZonedDateTime now = ZonedDateTime.now(TimeUtils.zoneId());
        boolean permanent = player.getPunishments().stream()
                .anyMatch(p -> p.getType() == type && p.isActive() && p.getEndDate() == null);
        ZonedDateTime deadline = player.getPunishments().stream()
                .filter(p -> p.getType() == type && p.isActive() && p.getEndDate() != null && p.getEndDate().isAfter(now))
                .map(Punishment::getEndDate).max(ZonedDateTime::compareTo).orElse(null);
        boolean hasExpired = player.getPunishments().stream().anyMatch(p -> p.getType() == type && p.isActive()
                && p.getEndDate() != null && !p.getEndDate().isAfter(now));
        setTimedFlag(player, type, permanent || deadline != null);
        if (hasExpired) expireRows(player, type, now, false);

        StateKey key = new StateKey(player.getUuid(), type);
        ScheduledTask old = timedTasks.remove(key);
        if (old != null) old.cancel();
        if (deadline == null) return;

        long ticks = Math.max(1L, ChronoUnit.MILLIS.between(now, deadline) / 50L);
        ScheduledTask replacement = plugin.getApi().scheduler().runGlobalLater(task ->
        {
            if (timedTasks.remove(key, task)) expireRows(player, type, ZonedDateTime.now(TimeUtils.zoneId()), true);
        }, ticks);
        timedTasks.put(key, replacement);
    }

    private void expireRows(PlexPlayer player, PunishmentType type, ZonedDateTime now, boolean announce)
    {
        boolean stillActive = player.getPunishments().stream().anyMatch(p -> p.getType() == type && p.isActive()
                && (p.getEndDate() == null || p.getEndDate().isAfter(now)));
        setTimedFlag(player, type, stillActive);
        plugin.getPunishmentRepository().expirePunishments(type, player.getUuid(), now.toInstant()).whenComplete((unused, failure) ->
        {
            if (failure != null)
            {
                PlexLog.error("Failed to expire {0} for {1}: {2}", type, player.getUuid(), failure.getMessage());
                return;
            }
            PlexPlayer current = plugin.getPlayerService().getCachedPlayer(player.getUuid());
            if (current == null) return;
            current.getPunishments().stream()
                    .filter(p -> p.getType() == type && p.getEndDate() != null && !p.getEndDate().isAfter(now))
                    .forEach(p -> p.setActive(false));
            if (announce && !isTimedActive(current, type))
            {
                Bukkit.broadcast(PlexUtils.messageComponent(type == PunishmentType.MUTE ? "unmutedPlayer" : "unfrozePlayer",
                        "Plex", Bukkit.getOfflinePlayer(player.getUuid()).getName()));
            }
        });
    }

    private static boolean isTimedActive(PlexPlayer player, PunishmentType type)
    {
        ZonedDateTime now = ZonedDateTime.now(TimeUtils.zoneId());
        return player.getPunishments().stream().anyMatch(p -> p.getType() == type && p.isActive()
                && (p.getEndDate() == null || p.getEndDate().isAfter(now)));
    }

    private static void setTimedFlag(PlexPlayer player, PunishmentType type, boolean active)
    {
        if (type == PunishmentType.MUTE) player.setMuted(active); else player.setFrozen(active);
    }

    private void publishInvalidation(UUID uuid, @Nullable String ip)
    {
        dev.plex.util.redis.MessageUtil.publishBanInvalidation(plugin, uuid, ip)
                .exceptionally(failure ->
                {
                    PlexLog.warn("Unable to publish ban cache invalidation: {0}", failure.getMessage());
                    return null;
                });
    }

    private boolean hasBanBypass(UUID uuid)
    {
        return plugin.getPermissions() != null && plugin.getPermissions().playerHas(null,
                Bukkit.getOfflinePlayer(uuid), "plex.ban.bypass");
    }

    private String nextIndefiniteBanKey(String type, String value)
    {
        String slug = value.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (slug.isEmpty()) slug = "entry";
        if (slug.length() > 40) slug = slug.substring(0, 40);
        String base = "command-" + type + "-" + slug;
        String key = base;
        int suffix = 2;
        while (plugin.indefBans.contains(key))
        {
            key = base + "-" + suffix++;
        }
        return key;
    }

    @Getter
    public static final class IndefiniteBan
    {
        private final List<String> usernames;
        private final List<UUID> uuids;
        private final List<String> ips;
        private final String reason;

        public IndefiniteBan(List<String> usernames, List<UUID> uuids, List<String> ips, String reason)
        {
            this.usernames = List.copyOf(usernames);
            this.uuids = List.copyOf(uuids);
            this.ips = List.copyOf(ips);
            this.reason = reason == null ? "" : reason;
        }
    }

    private record StateKey(UUID uuid, PunishmentType type) { }
}
