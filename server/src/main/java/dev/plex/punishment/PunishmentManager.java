package dev.plex.punishment;

import com.google.common.net.InetAddresses;
import lombok.AccessLevel;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import org.bukkit.Bukkit;

import dev.plex.Plex;
import dev.plex.api.punishment.IndefiniteBanRequest;
import dev.plex.api.punishment.PunishmentType;
import dev.plex.api.punishment.PunishmentSource;
import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;
import dev.plex.config.Config;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.admission.BanDecisionService;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import dev.plex.util.TimeUtils;
import dev.plex.util.redis.MessageUtil;
import io.papermc.paper.event.player.PlayerServerFullCheckEvent;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import lombok.Getter;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class PunishmentManager
{
    private final Plex plugin;
    private final BanDecisionService banDecisionService;
    private final FiniteBanEnforcement finiteBanEnforcement;
    private volatile List<IndefiniteBan> indefiniteBans = List.of();
    private CompletableFuture<?> indefiniteBanWrites = CompletableFuture.completedFuture(null);
    private final ConcurrentHashMap<StateKey, ScheduledTask> timedTasks = new ConcurrentHashMap<>();
    private ScheduledTask targetRefreshTask;
    private static final UUID TARGET_INVALIDATION = new UUID(0L, 0L);

    public PunishmentManager(Plex plugin)
    {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        int cacheSize = Math.max(100, plugin.config.getInt("banning.admission-cache-size", 10_000));
        long ttlSeconds = Math.max(1, plugin.config.getLong("banning.admission-cache-seconds", 60));
        this.banDecisionService = new BanDecisionService(plugin.getPunishmentRepository(), Duration.ofSeconds(ttlSeconds), cacheSize);
        this.finiteBanEnforcement = new FiniteBanEnforcement(plugin, this);
    }

    public void start()
    {
        banDecisionService.reloadTargetBans().join();
        long seconds = Math.max(1, plugin.config.getLong("banning.admission-cache-seconds", 60));
        targetRefreshTask = Bukkit.getAsyncScheduler().runAtFixedRate(plugin,
                task -> reloadTargetBans(), seconds, seconds, TimeUnit.SECONDS);
    }

    public void close()
    {
        if (targetRefreshTask != null) targetRefreshTask.cancel();
        timedTasks.values().forEach(ScheduledTask::cancel);
    }

    private void reloadTargetBans()
    {
        banDecisionService.reloadTargetBans().thenCompose(unused -> finiteBanEnforcement.enforceTargetBans())
                .exceptionally(failure ->
                {
                    PlexLog.error("Unable to reload active IP and name bans", failure);
                    return null;
                });
    }

    public CompletableFuture<Boolean> banIp(BanIpRange range, String reason, UUID punisher, String punisherName)
    {
        return banTarget(TargetBan.Kind.IP, range.toString(), range, reason, punisher, punisherName);
    }

    public CompletableFuture<Boolean> banUsername(String username, String reason, UUID punisher, String punisherName)
    {
        return banTarget(TargetBan.Kind.NAME, username.toLowerCase(Locale.ROOT), null, reason, punisher, punisherName);
    }

    private CompletableFuture<Boolean> banTarget(TargetBan.Kind kind, String target, BanIpRange range,
                                                  String reason, UUID punisher, String punisherName)
    {
        Instant now = Instant.now();
        TargetBan ban = new TargetBan(kind, target, reason, punisher,
                punisher == null ? PunishmentSource.CONSOLE : PunishmentSource.PLAYER, null,
                now, now.plus(PunishmentType.STANDARD_BAN_DURATION), range,
                punisherName);
        return banDecisionService.mutateTargetBans(() -> plugin.getPunishmentRepository().insertTargetBan(ban).thenApply(changed ->
        {
            if (!changed) return false;
            banDecisionService.addTargetBan(ban);
            publishInvalidation(TARGET_INVALIDATION, null);
            return true;
        })).thenCompose(changed ->
        {
            if (!changed) return CompletableFuture.completedFuture(false);
            return finiteBanEnforcement.enforceTargetBans().thenApply(unused -> true);
        });
    }

    public CompletableFuture<Boolean> unbanIp(BanIpRange range)
    {
        return unbanTarget(TargetBan.Kind.IP, range.toString());
    }

    public CompletableFuture<Boolean> unbanUsername(String username)
    {
        return unbanTarget(TargetBan.Kind.NAME, username.toLowerCase(Locale.ROOT));
    }

    private CompletableFuture<Boolean> unbanTarget(TargetBan.Kind kind, String target)
    {
        return banDecisionService.mutateTargetBans(() -> plugin.getPunishmentRepository()
                .deactivateTargetBan(kind, target, Instant.now()).thenApply(changed ->
        {
            if (!changed) return false;
            banDecisionService.removeTargetBan(kind, target);
            publishInvalidation(TARGET_INVALIDATION, null);
            return true;
        }));
    }

    public CompletableFuture<Optional<Punishment>> decideTargetAdmission(UUID uuid, String username, String ip)
    {
        Optional<Punishment> target = banDecisionService.targetBan(uuid, username, ip);
        if (target.isEmpty()) return CompletableFuture.completedFuture(target);
        return CompletableFuture.supplyAsync(() -> hasBanBypass(uuid) ? Optional.empty() : target,
                plugin.getIoExecutor());
    }

    public CompletableFuture<Admission> decideAdmission(UUID uuid, String username, String ip)
    {
        return CompletableFuture.supplyAsync(() -> hasBanBypass(uuid), plugin.getIoExecutor()).thenCompose(bypass ->
        {
            if (bypass) return CompletableFuture.completedFuture(new Admission(Optional.empty(), false));
            Optional<Punishment> target = banDecisionService.targetBan(uuid, username, ip);
            if (target.isPresent()) return CompletableFuture.completedFuture(new Admission(target, true));
            return banDecisionService.decide(uuid, ip).thenApply(ban -> new Admission(ban, false));
        });
    }

    public record Admission(Optional<Punishment> punishment, boolean denyLogin) { }

    // Reloads and API writes hold the indefbans.yml monitor while they change its configuration tree and this list.
    public void mergeIndefiniteBans()
    {
        List<IndefiniteBan> loaded;
        synchronized (plugin.indefBans)
        {
            loaded = readIndefiniteBans();
            indefiniteBans = loaded;
        }

        PlexLog.log("Loaded {0} UUID(s), {1} IP(s), and {2} username(s) as indefinitely banned",
                loaded.stream().map(IndefiniteBan::getUuids).mapToLong(Collection::size).sum(),
                loaded.stream().map(IndefiniteBan::getIps).mapToLong(Collection::size).sum(),
                loaded.stream().map(IndefiniteBan::getUsernames).mapToLong(Collection::size).sum());
    }

    private List<IndefiniteBan> readIndefiniteBans()
    {
        Config file = plugin.indefBans;
        return file.getKeys(false).stream().map(label -> IndefiniteBan.fromFile(label,
                file.getStringList(label + ".users"),
                file.getStringList(label + ".uuids").stream().map(UUID::fromString).toList(),
                file.getStringList(label + ".ips"),
                file.getString(label + ".reason", ""),
                file.getString(label + ".message", ""))).toList();
    }

    public CompletableFuture<Boolean> createIndefiniteBan(IndefiniteBan ban)
    {
        IndefiniteBan.requireSimpleLabel(ban.getLabel());
        return writeIndefiniteBan(ban.getLabel(), ban, false);
    }

    public CompletableFuture<Boolean> updateIndefiniteBan(IndefiniteBan ban)
    {
        return writeIndefiniteBan(ban.getLabel(), ban, true);
    }

    public CompletableFuture<Boolean> deleteIndefiniteBan(String label)
    {
        return writeIndefiniteBan(label, null, true);
    }

    // Writes run one at a time in call order on the I/O executor. The next write and the caller's future each start
    // as a separate I/O task when a write finishes, so a caller callback that waits for a later write cannot hold up
    // that write.
    private synchronized CompletableFuture<Boolean> writeIndefiniteBan(String label, @Nullable IndefiniteBan ban,
                                                                      boolean existing)
    {
        CompletableFuture<Boolean> written = indefiniteBanWrites.handleAsync((unused, previousFailure) ->
        {
            Config file = plugin.indefBans;
            synchronized (file)
            {
                Config.Change<List<IndefiniteBan>> saved = file.saveChange(
                        contents -> IndefiniteBanYaml.write(contents, label, ban, existing), this::readIndefiniteBans);
                indefiniteBans = saved.reloaded();
                return saved.written();
            }
        }, plugin.getIoExecutor());
        indefiniteBanWrites = written;
        return written.whenCompleteAsync((changed, failure) -> { }, plugin.getIoExecutor());
    }

    public List<IndefiniteBan> getIndefiniteBans() { return indefiniteBans; }

    @Nullable
    public IndefiniteBan getIndefiniteBanByLabel(String label)
    {
        return indefiniteBans.stream().filter(ban -> ban.getLabel().equals(label)).findFirst().orElse(null);
    }

    @Nullable
    public IndefiniteBan getIndefiniteBanByUUID(UUID uuid)
    {
        return indefiniteBans.stream().filter(ban -> ban.getUuids().contains(uuid)).findFirst().orElse(null);
    }

    @Nullable
    public IndefiniteBan getIndefiniteBanByIP(String ip)
    {
        String canonicalIp = BanDecisionService.canonicalIp(ip);
        if (!InetAddresses.isInetAddress(canonicalIp)) return null;
        BanIpRange address = BanIpRange.parse(BanIpRange.banMatchKey(canonicalIp));
        return indefiniteBans.stream().filter(ban -> ban.ipRanges.stream()
                .anyMatch(range -> range.contains(address))).findFirst().orElse(null);
    }

    @Nullable
    public IndefiniteBan getIndefiniteBanByUsername(String username)
    {
        return indefiniteBans.stream().filter(ban -> ban.getUsernames().stream()
                .anyMatch(name -> name.equalsIgnoreCase(username))).findFirst().orElse(null);
    }

    public CompletableFuture<Optional<Punishment>> decideAdmission(UUID uuid, @Nullable String ip)
    {
        return decideAdmission(uuid, null, ip).thenApply(Admission::punishment);
    }

    CompletableFuture<Optional<Punishment>> decideFiniteAdmission(UUID uuid, @Nullable String ip)
    {
        return CompletableFuture.supplyAsync(() -> hasBanBypass(uuid), plugin.getIoExecutor())
                .thenCompose(hasBypass -> hasBypass
                        ? CompletableFuture.completedFuture(Optional.empty())
                        : banDecisionService.decide(uuid, ip));
    }

    public synchronized void invalidateBanDecisions(UUID uuid, @Nullable String ip)
    {
        banDecisionService.invalidate(uuid, ip);
    }

    public void handleBanInvalidation(UUID uuid, @Nullable String ip)
    {
        if (TARGET_INVALIDATION.equals(uuid))
        {
            reloadTargetBans();
            return;
        }
        invalidateBanDecisions(uuid, ip);
        finiteBanEnforcement.refreshMatching(uuid, ip).exceptionally(failure ->
        {
            PlexLog.warn("Unable to refresh online ban state for {0}: {1}", uuid, failure.getMessage());
            return null;
        });
    }

    public void resynchronizeBans()
    {
        synchronized (this)
        {
            banDecisionService.invalidateAll();
        }
        reloadTargetBans();
        finiteBanEnforcement.refreshAll().exceptionally(failure ->
        {
            PlexLog.error("Unable to refresh online ban state after resynchronizing bans", failure);
            return null;
        });
    }

    public synchronized long prepareFiniteBanAdmission(UUID uuid, String ip, @Nullable Punishment punishment,
                                                       BanDecisionService.Revision decisionRevision)
    {
        if (!banDecisionService.revision(uuid, ip).equals(decisionRevision)) return -2L;
        return finiteBanEnforcement.prepareAdmission(uuid, ip, punishment);
    }

    public BanDecisionService.Revision banDecisionRevision(UUID uuid, String ip)
    {
        return banDecisionService.revision(uuid, ip);
    }

    public void cancelPendingAdmission(UUID uuid, long token)
    {
        finiteBanEnforcement.cancelPendingAdmission(uuid, token);
    }

    public void cancelPendingAdmission(UUID uuid)
    {
        finiteBanEnforcement.cancelPendingAdmission(uuid);
    }

    public void checkAdmissionCapacity(PlayerServerFullCheckEvent event)
    {
        finiteBanEnforcement.checkCapacity(event);
    }

    public void completeJoin(Player player)
    {
        finiteBanEnforcement.join(player);
        finiteBanEnforcement.enforceTargetBan(player).exceptionally(failure ->
        {
            PlexLog.error("Unable to enforce IP or name ban after joining", failure);
            return null;
        });
    }

    public void trackReloadedPlayer(Player player, String ip)
    {
        finiteBanEnforcement.trackReloadedPlayer(player, ip);
    }

    public void trackOnlineCapacity(Player player, String ip)
    {
        finiteBanEnforcement.trackOnlineCapacity(player, ip);
    }

    public void closePendingAdmission(PlayerConnectionCloseEvent event)
    {
        finiteBanEnforcement.connectionClosed(event);
    }

    public void completeQuit(UUID uuid)
    {
        finiteBanEnforcement.quit(uuid);
    }

    public boolean isFiniteBanRestricted(UUID uuid)
    {
        return finiteBanEnforcement.isRestricted(uuid);
    }

    public Set<UUID> finiteBanRestrictedOnlinePlayers()
    {
        return finiteBanEnforcement.restrictedOnlinePlayers();
    }

    public Component finiteBanMessage(UUID uuid)
    {
        return finiteBanEnforcement.restrictionMessage(uuid);
    }

    public void restoreFiniteBanInventory(Player player)
    {
        finiteBanEnforcement.restoreInventory(player);
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
        finiteBanEnforcement.beginBanRemoval(uuid);
        return plugin.getPunishmentRepository().removeBan(uuid).thenCompose(removal ->
        {
            if (!removal.changed())
            {
                invalidateBanDecisions(uuid, null);
                return finiteBanEnforcement.refreshBanOwner(uuid).thenApply(unused -> false);
            }
            invalidateBanDecisions(uuid, null);
            List<CompletableFuture<Void>> refreshes = new ArrayList<>();
            refreshes.add(finiteBanEnforcement.refreshMatching(uuid, null));
            refreshes.add(finiteBanEnforcement.refreshBanOwner(uuid));
            for (String ip : removal.ips())
            {
                invalidateBanDecisions(uuid, ip);
                refreshes.add(finiteBanEnforcement.refreshMatching(uuid, ip));
            }
            return CompletableFuture.allOf(refreshes.toArray(CompletableFuture[]::new)).thenApply(unused ->
            {
                PlexPlayer player = plugin.getPlayerService().cachedPlayer(uuid);
                if (player != null)
                {
                    player.getPunishments().stream().filter(p -> p.getType().isBan()).forEach(p -> p.setActive(false));
                }
                if (removal.ips().isEmpty()) publishInvalidation(uuid, null);
                else removal.ips().forEach(ip -> publishInvalidation(uuid, ip));
                return true;
            });
        }).whenComplete((unused, failure) -> finiteBanEnforcement.finishBanRemoval(uuid));
    }

    public CompletableFuture<Void> punish(PlexPlayer player, Punishment punishment)
    {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(punishment, "punishment");
        IllegalArgumentException invalid = validateForPersistence(player, punishment);
        if (invalid != null)
        {
            return CompletableFuture.failedFuture(invalid);
        }
        if (punishment.getIp() != null) punishment.setIp(BanDecisionService.canonicalIp(punishment.getIp()));
        punishment.setActive(punishment.getType().startsActive());

        // Only the target's own ban blocks a new one. An IP ban on another account
        // sharing the address must not stop this player from receiving their own record.
        CompletableFuture<Boolean> alreadyActive = punishment.getType().isBan()
                ? isBanned(player.getUuid())
                : CompletableFuture.completedFuture(false);
        return alreadyActive.thenCompose(active ->
        {
            if (active)
            {
                return CompletableFuture.failedFuture(new IllegalStateException("Player is already banned"));
            }
            return plugin.getPunishmentRepository().insertPunishment(punishment);
        }).thenCompose(unused ->
        {
            player.getPunishments().add(punishment);
            if (punishment.getType().isBan())
            {
                invalidateBanDecisions(player.getUuid(), punishment.getIp());
                return finiteBanEnforcement.refreshMatching(player.getUuid(), punishment.getIp()).thenRun(() ->
                        publishInvalidation(player.getUuid(), punishment.getIp()));
            }
            if (punishment.getType() == PunishmentType.MUTE || punishment.getType() == PunishmentType.FREEZE)
                restoreTimedState(player, punishment.getType());
            return CompletableFuture.completedFuture(null);
        });
    }

    @Nullable
    private IllegalArgumentException validateForPersistence(PlexPlayer player, Punishment punishment)
    {
        if (!player.getUuid().equals(punishment.getPunished()))
            return new IllegalArgumentException("Punishment and player UUIDs differ");
        if (punishment.getType() == null)
            return new IllegalArgumentException("Punishment type is required");
        if (punishment.getIssueDate() == null)
            return new IllegalArgumentException("Punishment issue date is required");

        punishment.getType().fixedDuration().ifPresent(duration ->
                punishment.setEndDate(punishment.getIssueDate().plus(duration)));
        ZonedDateTime endDate = punishment.getEndDate();
        if (punishment.getType().requiresEndDate() && endDate == null)
            return new IllegalArgumentException(punishment.getType() + " requires an end date");
        if (!punishment.getType().requiresEndDate() && endDate != null)
            return new IllegalArgumentException(punishment.getType() + " must not have an end date");
        if (endDate != null && !endDate.toInstant().isAfter(Instant.now()))
            return new IllegalArgumentException(punishment.getType() + " requires a future end date");
        Duration maximumDuration = punishment.getType().maximumDuration().orElse(null);
        if (endDate != null && maximumDuration != null
                && endDate.toInstant().isAfter(punishment.getIssueDate().toInstant().plus(maximumDuration)))
            return new IllegalArgumentException(punishment.getType() + " exceeds its maximum duration");
        if (punishment.getReason() == null)
            return new IllegalArgumentException("Punishment reason is required");
        return null;
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
        ZonedDateTime now = ZonedDateTime.now(TimeUtils.zoneId());
        List<Punishment> active = player.getPunishments().stream()
                .filter(punishment -> punishment.getType() == type)
                .filter(Punishment::isActive)
                .toList();
        ZonedDateTime deadline = active.stream().map(Punishment::getEndDate)
                .filter(endDate -> endDate.isAfter(now))
                .max(ZonedDateTime::compareTo).orElse(null);
        boolean hasExpired = active.stream().map(Punishment::getEndDate)
                .anyMatch(endDate -> !endDate.isAfter(now));
        setTimedFlag(player, type, deadline != null);
        if (hasExpired) expireRows(player, type, now, false);

        StateKey key = new StateKey(player.getUuid(), type);
        ScheduledTask old = timedTasks.remove(key);
        if (old != null) old.cancel();
        if (deadline == null) return;

        long delayMillis = Math.max(1L, ChronoUnit.MILLIS.between(now, deadline));
        ScheduledTask replacement = Bukkit.getAsyncScheduler().runDelayed(plugin, task ->
        {
            if (timedTasks.remove(key, task)) expireRows(player, type, ZonedDateTime.now(TimeUtils.zoneId()), true);
        }, delayMillis, TimeUnit.MILLISECONDS);
        timedTasks.put(key, replacement);
    }

    private void expireRows(PlexPlayer player, PunishmentType type, ZonedDateTime now, boolean announce)
    {
        setTimedFlag(player, type, isTimedActive(player, type));
        plugin.getPunishmentRepository().expirePunishments(type, player.getUuid(), now.toInstant()).whenComplete((unused, failure) ->
        {
            if (failure != null)
            {
                PlexLog.error("Failed to expire {0} for {1}: {2}", type, player.getUuid(), failure.getMessage());
                return;
            }
            PlexPlayer current = plugin.getPlayerService().cachedPlayer(player.getUuid());
            if (current == null) return;
            current.getPunishments().stream()
                    .filter(p -> p.getType() == type && p.getEndDate() != null && !p.getEndDate().isAfter(now))
                    .forEach(p -> p.setActive(false));
            if (announce && !isTimedActive(current, type))
            {
                Bukkit.broadcast(PlexUtils.messageComponent(type == PunishmentType.MUTE ? "unmutedPlayer" : "unfrozePlayer",
                        Placeholder.parsed("sender", "Plex"), Placeholder.parsed("player", String.valueOf(Bukkit.getOfflinePlayer(player.getUuid()).getName()))));
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
        MessageUtil.publishBanInvalidation(plugin, uuid, ip)
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

    @Getter
    public static final class IndefiniteBan
    {
        private static final int MAX_LABEL_LENGTH = 122;

        private final String label;
        private final List<String> usernames;
        private final List<UUID> uuids;
        private final List<String> ips;
        @Getter(AccessLevel.NONE)
        private final List<BanIpRange> ipRanges;
        private final String reason;
        // MiniMessage text shown as the whole ban screen, or empty for the messages.yml screen.
        private final String message;

        private IndefiniteBan(String label, List<String> usernames, List<UUID> uuids, List<String> ips,
                              List<BanIpRange> ipRanges, String reason, String message)
        {
            this.label = label;
            this.usernames = List.copyOf(usernames);
            this.uuids = List.copyOf(uuids);
            this.ips = List.copyOf(ips);
            this.ipRanges = List.copyOf(ipRanges);
            this.reason = reason == null ? "" : reason;
            this.message = message;
        }

        // A hand-edited entry keeps its valid IPs; an API request with an invalid IP is rejected instead.
        private static IndefiniteBan fromFile(String label, List<String> usernames, List<UUID> uuids, List<String> ips,
                                              String reason, String message)
        {
            List<String> validIps = new ArrayList<>();
            List<BanIpRange> ranges = new ArrayList<>();
            for (String ip : ips)
            {
                try
                {
                    ranges.add(BanIpRange.parse(ip));
                    validIps.add(ip);
                }
                catch (IllegalArgumentException exception)
                {
                    PlexLog.warn("Skipped invalid indefinite ban IP entry [{0}] in indefbans.yml: {1}", ip, exception.getMessage());
                }
            }
            return new IndefiniteBan(label, usernames, uuids, validIps, ranges, reason, message);
        }

        public static IndefiniteBan fromRequest(IndefiniteBanRequest request)
        {
            List<BanIpRange> ranges = request.ips().stream().map(BanIpRange::parse).toList();
            return new IndefiniteBan(requireValidLabel(request.label()), request.usernames(), request.uuids(),
                    request.ips(), ranges, request.reason(), request.message());
        }

        // Any label the file loader accepts, so update and delete reach every entry a hand-edited file can hold.
        public static String requireValidLabel(String label)
        {
            if (!Config.isValidIndefiniteBanLabel(label))
            {
                throw new IllegalArgumentException("Invalid indefinite ban label '" + label + "': labels must be non-empty and cannot contain dots.");
            }
            return label;
        }

        // Create writes the label as a simple key. SnakeYAML writes a key as an explicit "? " key once it reaches
        // 128 characters, counting the 5 of its !!str tag, or when it contains a line break.
        private static void requireSimpleLabel(String label)
        {
            if (label.length() > MAX_LABEL_LENGTH)
            {
                throw new IllegalArgumentException("Invalid indefinite ban label of " + label.length()
                        + " characters: a new label can have at most " + MAX_LABEL_LENGTH + " characters.");
            }
            if (label.chars().anyMatch(c -> IndefiniteBanYaml.isLineBreak((char) c)))
            {
                throw new IllegalArgumentException("Invalid indefinite ban label: a new label cannot contain line breaks.");
            }
        }
    }

    private record StateKey(UUID uuid, PunishmentType type) { }
}
