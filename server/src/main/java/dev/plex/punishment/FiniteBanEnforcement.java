package dev.plex.punishment;

import org.bukkit.Bukkit;


import com.destroystokyo.paper.event.player.PlayerConnectionCloseEvent;
import dev.plex.Plex;
import dev.plex.punishment.admission.BanDecisionService;
import dev.plex.util.BungeeUtil;
import dev.plex.util.PlexUtils;
import io.papermc.paper.event.player.PlayerServerFullCheckEvent;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.GameMode;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

final class FiniteBanEnforcement
{
    private final Plex plugin;
    private final PunishmentManager punishmentManager;
    private final NamespacedKey previousGameModeKey;
    private final Map<UUID, PendingAdmission> pendingAdmissions = new LinkedHashMap<>();
    private final Map<UUID, Reservation> reservations = new LinkedHashMap<>();
    private final Map<UUID, OnlinePlayer> onlinePlayers = new LinkedHashMap<>();
    private final Map<UUID, OnlineRestriction> restrictions = new LinkedHashMap<>();
    private final Set<UUID> evicting = new LinkedHashSet<>();
    private final Set<UUID> protectedFromEviction = new LinkedHashSet<>();
    private final Map<UUID, Integer> removingBanOwners = new LinkedHashMap<>();
    private final Set<UUID> unresolvedAdmissions = new LinkedHashSet<>();
    private final Set<UUID> restoringInventories = new LinkedHashSet<>();
    private final Map<UUID, Long> refreshVersions = new LinkedHashMap<>();
    private final Map<UUID, Integer> joinedCloseDebts = new LinkedHashMap<>();
    private long nextAdmissionToken;

    FiniteBanEnforcement(Plex plugin, PunishmentManager punishmentManager)
    {
        this.plugin = plugin;
        this.punishmentManager = punishmentManager;
        this.previousGameModeKey = new NamespacedKey(plugin, "pre-ban-gamemode");
    }

    synchronized long prepareAdmission(UUID uuid, String ip, @Nullable Punishment punishment)
    {
        if (pendingAdmissions.containsKey(uuid)) return -1L;
        long token = ++nextAdmissionToken;
        PendingAdmission admission = new PendingAdmission(BanDecisionService.canonicalIp(ip), punishment, token);
        pendingAdmissions.put(uuid, admission);
        return token;
    }

    synchronized void cancelPendingAdmission(UUID uuid, long token)
    {
        PendingAdmission admission = pendingAdmissions.get(uuid);
        if (admission == null || admission.token() != token) return;
        pendingAdmissions.remove(uuid);
        unresolvedAdmissions.remove(uuid);
        releaseReservation(uuid);
    }

    synchronized void cancelPendingAdmission(UUID uuid)
    {
        pendingAdmissions.remove(uuid);
        unresolvedAdmissions.remove(uuid);
        releaseReservation(uuid);
    }

    synchronized void checkCapacity(PlayerServerFullCheckEvent event)
    {
        UUID uuid = event.getPlayerProfile().getId();
        if (uuid == null) return;
        Reservation existing = reservations.get(uuid);
        if (existing != null && existing.allowed())
        {
            applyReservation(event, existing, pendingAdmissions.get(uuid));
            return;
        }
        if (existing != null) reservations.remove(uuid);

        PendingAdmission admission = pendingAdmissions.get(uuid);
        if (unresolvedAdmissions.contains(uuid))
        {
            event.deny(Component.text("Unable to verify your updated ban status. Please reconnect."));
            return;
        }
        boolean banned = admission != null && isEffective(admission.punishment());
        int maximum = plugin.getServer().getMaxPlayers();
        int occupancy = onlinePlayers.size() + (int) reservations.values().stream().filter(Reservation::allowed).count();
        if (banned)
        {
            boolean allowed = event.isAllowed() && occupancy < Math.max(0, maximum - 1);
            Reservation reservation = new Reservation(allowed, null);
            reservations.put(uuid, reservation);
            applyReservation(event, reservation, admission);
            return;
        }

        if (event.isAllowed() && occupancy < maximum)
        {
            reservations.put(uuid, new Reservation(true, null));
            return;
        }

        UUID victim = oldestAvailableRestriction();
        if (victim == null)
        {
            reservations.put(uuid, new Reservation(false, null));
            return;
        }
        evicting.add(victim);
        reservations.put(uuid, new Reservation(true, victim));
        event.allow(true);
    }

    void join(Player player)
    {
        JoinPlan plan = planJoin(player);

        if (plan.rejectIncoming())
        {
            player.getScheduler().run(plugin, task -> player.kick(Component.translatable("multiplayer.disconnect.server_full")), null);
            return;
        }

        if (plan.admission() != null && isEffective(plan.admission().punishment()))
        {
            activate(player, plan.admission().ip(), plan.admission().punishment(), plan.admissionVersion());
        }
        else
        {
            restorePreviousGameMode(player);
        }
        if (plan.victim() != null)
        {
            evict(plan.victim().player(), player);
        }
    }

    private synchronized JoinPlan planJoin(Player player)
    {
        UUID uuid = player.getUniqueId();
        PendingAdmission admission = pendingAdmissions.remove(uuid);
        boolean unresolved = unresolvedAdmissions.remove(uuid);
        Reservation reservation = reservations.remove(uuid);
        String ip = admission == null ? currentIp(player) : admission.ip();
        long connectionToken = admission == null ? 0L : admission.token();
        onlinePlayers.put(uuid, new OnlinePlayer(player, ip, connectionToken));
        long admissionVersion = refreshVersions.merge(uuid, 1L, Long::sum);
        OnlinePlayer victim = reservation == null ? null : reservedVictim(reservation);
        boolean rejectIncoming = unresolved || reservation != null && reservation.victim() != null
                && victim == null && onlinePlayers.size() > plugin.getServer().getMaxPlayers();
        if (rejectIncoming)
        {
            OnlinePlayer rejected = onlinePlayers.remove(uuid);
            if (rejected != null && rejected.connectionToken() > 0L)
                joinedCloseDebts.merge(uuid, 1, Integer::sum);
        }
        return new JoinPlan(admission, victim, rejectIncoming, admissionVersion);
    }

    @Nullable
    private OnlinePlayer reservedVictim(Reservation reservation)
    {
        UUID victimId = reservation.victim();
        if (victimId == null) return null;
        if (!canEvict(victimId) || !onlinePlayers.containsKey(victimId))
        {
            evicting.remove(victimId);
            victimId = oldestAvailableRestriction();
            if (victimId != null) evicting.add(victimId);
        }
        return victimId == null ? null : onlinePlayers.get(victimId);
    }

    void trackReloadedPlayer(Player player, String ip)
    {
        OnlinePlayer online;
        synchronized (this)
        {
            online = onlinePlayers.get(player.getUniqueId());
        }
        if (online != null) refresh(online);
    }

    synchronized void trackOnlineCapacity(Player player, String ip)
    {
        onlinePlayers.putIfAbsent(player.getUniqueId(),
                new OnlinePlayer(player, BanDecisionService.canonicalIp(ip), 1L));
    }

    synchronized void connectionClosed(PlayerConnectionCloseEvent event)
    {
        UUID uuid = event.getPlayerUniqueId();
        int debt = joinedCloseDebts.getOrDefault(uuid, 0);
        if (debt > 0)
        {
            if (debt == 1) joinedCloseDebts.remove(uuid); else joinedCloseDebts.put(uuid, debt - 1);
            return;
        }
        pendingAdmissions.remove(uuid);
        unresolvedAdmissions.remove(uuid);
        Reservation reservation = reservations.remove(uuid);
        if (reservation != null && reservation.victim() != null) evicting.remove(reservation.victim());
    }

    synchronized void quit(UUID uuid)
    {
        OnlinePlayer online = onlinePlayers.remove(uuid);
        if (online != null && online.connectionToken() > 0L) joinedCloseDebts.merge(uuid, 1, Integer::sum);
        evicting.remove(uuid);
        protectedFromEviction.remove(uuid);
        refreshVersions.remove(uuid);
        OnlineRestriction restriction = restrictions.remove(uuid);
        if (restriction != null && restriction.expiryTask != null) restriction.expiryTask.cancel();
    }

    synchronized boolean isRestricted(UUID uuid)
    {
        OnlineRestriction restriction = restrictions.get(uuid);
        return restriction != null && restriction.punishment.isActive();
    }

    synchronized Component restrictionMessage(UUID uuid)
    {
        OnlineRestriction restriction = restrictions.get(uuid);
        return restriction == null ? Component.empty() : Punishment.generateBanMessage(restriction.punishment,
                plugin.config.getString("banning.ban_url"));
    }

    void restoreInventory(Player player)
    {
        ItemStack[] contents;
        synchronized (this)
        {
            OnlineRestriction restriction = restrictions.get(player.getUniqueId());
            if (restriction == null || restriction.inventoryContents == null
                    || restoringInventories.contains(player.getUniqueId())) return;
            contents = cloneContents(restriction.inventoryContents);
            if (Arrays.equals(contents, player.getInventory().getContents())) return;
            restoringInventories.add(player.getUniqueId());
        }
        try
        {
            player.getInventory().setContents(contents);
        }
        finally
        {
            synchronized (this)
            {
                restoringInventories.remove(player.getUniqueId());
            }
        }
    }

    CompletableFuture<Void> refreshMatching(UUID uuid, @Nullable String ip)
    {
        String canonicalIp = BanDecisionService.canonicalIp(ip);
        List<OnlinePlayer> matches;
        List<PendingPlayer> pending;
        synchronized (this)
        {
            matches = onlinePlayers.entrySet().stream()
                    .filter(entry -> entry.getKey().equals(uuid) || (!canonicalIp.isEmpty() && canonicalIp.equals(entry.getValue().ip())))
                    .map(Map.Entry::getValue).toList();
            pending = pendingAdmissions.entrySet().stream()
                    .filter(entry -> entry.getKey().equals(uuid) || (!canonicalIp.isEmpty() && canonicalIp.equals(entry.getValue().ip())))
                    .map(entry -> new PendingPlayer(entry.getKey(), entry.getValue().ip())).toList();
            pending.forEach(player ->
            {
                unresolvedAdmissions.add(player.uuid());
                releaseReservation(player.uuid());
            });
        }
        List<CompletableFuture<Void>> updates = new ArrayList<>();
        matches.forEach(online -> updates.add(refreshUntilResolved(online)));
        pending.forEach(player -> updates.add(refreshPendingUntilResolved(player)));
        return CompletableFuture.allOf(updates.toArray(CompletableFuture[]::new));
    }

    synchronized void beginBanRemoval(UUID owner)
    {
        removingBanOwners.merge(owner, 1, Integer::sum);
    }

    synchronized void finishBanRemoval(UUID owner)
    {
        removingBanOwners.computeIfPresent(owner, (uuid, count) -> count == 1 ? null : count - 1);
    }

    CompletableFuture<Void> refreshBanOwner(UUID owner)
    {
        List<OnlinePlayer> matches;
        List<PendingPlayer> pending;
        synchronized (this)
        {
            matches = restrictions.entrySet().stream()
                    .filter(entry -> entry.getValue().punishment.getPunished().equals(owner))
                    .map(entry -> onlinePlayers.get(entry.getKey()))
                    .filter(java.util.Objects::nonNull).toList();
            pending = pendingAdmissions.entrySet().stream()
                    .filter(entry -> entry.getValue().punishment() != null
                            && entry.getValue().punishment().getPunished().equals(owner))
                    .map(entry -> new PendingPlayer(entry.getKey(), entry.getValue().ip())).toList();
        }
        List<CompletableFuture<Void>> updates = new ArrayList<>();
        matches.forEach(player -> updates.add(refreshUntilResolved(player)));
        pending.forEach(player -> updates.add(refreshPendingUntilResolved(player)));
        return CompletableFuture.allOf(updates.toArray(CompletableFuture[]::new));
    }

    private CompletableFuture<Void> refresh(OnlinePlayer online)
    {
        long version;
        UUID uuid = online.player().getUniqueId();
        synchronized (this)
        {
            if (onlinePlayers.get(uuid) != online) return CompletableFuture.completedFuture(null);
            version = refreshVersions.merge(uuid, 1L, Long::sum);
            protectedFromEviction.add(uuid);
        }
        return punishmentManager.decideAdmission(online.player().getUniqueId(), online.ip()).thenCompose(ban ->
        {
            synchronized (this)
            {
                if (onlinePlayers.get(uuid) != online || refreshVersions.getOrDefault(uuid, 0L) != version)
                    return CompletableFuture.completedFuture(null);
            }
            return ban.isPresent() ? activate(online.player(), online.ip(), ban.get(), version)
                    : release(online.player(), version);
        }).whenComplete((unused, failure) ->
        {
            if (failure == null) return;
            OnlinePlayer retry = null;
            synchronized (this)
            {
                if (refreshVersions.getOrDefault(uuid, 0L) == version)
                {
                    protectedFromEviction.remove(uuid);
                    if (evicting.contains(uuid)) retry = onlinePlayers.get(uuid);
                }
            }
            if (retry != null) evict(retry.player(), null);
        });
    }

    private CompletableFuture<Void> refreshUntilResolved(OnlinePlayer online)
    {
        CompletableFuture<Void> completion = new CompletableFuture<>();
        refreshUntilResolved(online, completion);
        return completion;
    }

    private void refreshUntilResolved(OnlinePlayer online, CompletableFuture<Void> completion)
    {
        refresh(online).whenComplete((unused, failure) ->
        {
            if (failure == null)
            {
                completion.complete(null);
                return;
            }
            Bukkit.getAsyncScheduler().runDelayed(plugin,
                    ignored -> refreshUntilResolved(online, completion), 1L, TimeUnit.SECONDS);
        });
    }

    private CompletableFuture<Void> refreshPending(PendingPlayer player)
    {
        synchronized (this)
        {
            PendingAdmission current = pendingAdmissions.get(player.uuid());
            if (current == null || !current.ip().equals(player.ip())) return CompletableFuture.completedFuture(null);
        }
        return punishmentManager.decideAdmission(player.uuid(), player.ip()).thenCompose(ban ->
        {
            OnlinePlayer joined;
            synchronized (this)
            {
                PendingAdmission current = pendingAdmissions.get(player.uuid());
                if (current != null && current.ip().equals(player.ip()))
                {
                    pendingAdmissions.put(player.uuid(), new PendingAdmission(player.ip(), ban.orElse(null), current.token()));
                    unresolvedAdmissions.remove(player.uuid());
                    releaseReservation(player.uuid());
                    return CompletableFuture.completedFuture(null);
                }
                joined = onlinePlayers.get(player.uuid());
            }
            return joined == null ? CompletableFuture.completedFuture(null) : refresh(joined);
        });
    }

    private CompletableFuture<Void> refreshPendingUntilResolved(PendingPlayer player)
    {
        CompletableFuture<Void> completion = new CompletableFuture<>();
        refreshPendingUntilResolved(player, completion);
        return completion;
    }

    private void refreshPendingUntilResolved(PendingPlayer player, CompletableFuture<Void> completion)
    {
        refreshPending(player).whenComplete((unused, failure) ->
        {
            if (failure == null)
            {
                completion.complete(null);
                return;
            }
            Bukkit.getAsyncScheduler().runDelayed(plugin,
                    ignored -> refreshPendingUntilResolved(player, completion), 1L, TimeUnit.SECONDS);
        });
    }

    private CompletableFuture<Void> activate(Player player, String ip, Punishment punishment, long expectedVersion)
    {
        BossBar bar = BossBar.bossBar(Punishment.generateBanStatusMessage(punishment), 1.0f,
                BossBar.Color.RED, BossBar.Overlay.PROGRESS);
        OnlineRestriction replacement = new OnlineRestriction(punishment, bar);
        CompletableFuture<Void> completion = new CompletableFuture<>();
        ScheduledTask task = player.getScheduler().run(plugin, ignored ->
        {
            ActivationPlan plan = installRestriction(player, replacement, expectedVersion);
            if (plan == null)
            {
                completion.complete(null);
                return;
            }
            if (plan.previous() != null && plan.previous().expiryTask != null) plan.previous().expiryTask.cancel();
            scheduleExpiry(player.getUniqueId(), ip, replacement);
            if (!player.getPersistentDataContainer().has(previousGameModeKey, PersistentDataType.STRING))
            {
                player.getPersistentDataContainer().set(previousGameModeKey, PersistentDataType.STRING, player.getGameMode().name());
            }
            hidePreviousBossBars(player, plan.previous());
            player.showBossBar(bar);
            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(Punishment.generateBanMessage(punishment, plugin.config.getString("banning.ban_url")));
            if (plan.shouldEvict()) evict(player, null);
            if (!plan.wasRestricted()) enforceBuffer();
            completion.complete(null);
        }, () -> completion.complete(null));
        if (task == null) completion.complete(null);
        return completion;
    }

    private synchronized @Nullable ActivationPlan installRestriction(Player player, OnlineRestriction replacement,
                                                                      long expectedVersion)
    {
        OnlinePlayer online = onlinePlayers.get(player.getUniqueId());
        if (online == null || online.player() != player) return null;
        if (refreshVersions.getOrDefault(player.getUniqueId(), 0L) != expectedVersion) return null;
        OnlineRestriction previous = restrictions.get(player.getUniqueId());
        replacement.previous = previous;
        replacement.inventoryContents = inventorySnapshot(player, previous);
        restrictions.put(player.getUniqueId(), replacement);
        protectedFromEviction.remove(player.getUniqueId());
        return new ActivationPlan(previous, previous != null && isEffective(previous.punishment),
                evicting.contains(player.getUniqueId()));
    }

    private CompletableFuture<Void> release(Player player, long expectedVersion)
    {
        OnlineRestriction removed;
        synchronized (this)
        {
            if (refreshVersions.getOrDefault(player.getUniqueId(), 0L) != expectedVersion)
                return CompletableFuture.completedFuture(null);
            removed = restrictions.remove(player.getUniqueId());
            protectedFromEviction.remove(player.getUniqueId());
            evicting.remove(player.getUniqueId());
        }
        if (removed == null) return CompletableFuture.completedFuture(null);
        if (removed.expiryTask != null) removed.expiryTask.cancel();
        CompletableFuture<Void> completion = new CompletableFuture<>();
        ScheduledTask task = player.getScheduler().run(plugin, ignored ->
        {
            synchronized (this)
            {
                if (restrictions.containsKey(player.getUniqueId()))
                {
                    completion.complete(null);
                    return;
                }
            }
            player.hideBossBar(removed.bar);
            hidePreviousBossBars(player, removed.previous);
            restorePreviousGameModeNow(player);
            completion.complete(null);
        }, () -> completion.complete(null));
        if (task == null) completion.complete(null);
        return completion;
    }

    private void restorePreviousGameMode(Player player)
    {
        player.getScheduler().run(plugin, task -> restorePreviousGameModeNow(player), null);
    }

    private static void hidePreviousBossBars(Player player, @Nullable OnlineRestriction restriction)
    {
        OnlineRestriction current = restriction;
        while (current != null)
        {
            player.hideBossBar(current.bar);
            OnlineRestriction previous = current.previous;
            current.previous = null;
            current = previous;
        }
    }

    private void restorePreviousGameModeNow(Player player)
    {
        String stored = player.getPersistentDataContainer().get(previousGameModeKey, PersistentDataType.STRING);
        if (stored == null) return;
        player.getPersistentDataContainer().remove(previousGameModeKey);
        if (player.getGameMode() != GameMode.SPECTATOR) return;
        try
        {
            player.setGameMode(GameMode.valueOf(stored));
        }
        catch (IllegalArgumentException ignored)
        {
            player.setGameMode(plugin.getServer().getDefaultGameMode());
        }
    }

    private void scheduleExpiry(UUID uuid, String ip, OnlineRestriction restriction)
    {
        ZonedDateTime endDate = restriction.punishment.getEndDate();
        if (endDate == null) return;
        long millis = Math.max(1L, Duration.between(ZonedDateTime.now(endDate.getZone()), endDate).toMillis());
        ScheduledTask task = Bukkit.getAsyncScheduler().runDelayed(plugin,
                ignored -> refreshById(uuid, ip, restriction), millis, TimeUnit.MILLISECONDS);
        synchronized (this)
        {
            if (restrictions.get(uuid) == restriction) restriction.expiryTask = task;
            else task.cancel();
        }
    }

    private void refreshById(UUID uuid, String ip, OnlineRestriction expected)
    {
        synchronized (this)
        {
            if (restrictions.get(uuid) != expected) return;
        }
        punishmentManager.invalidateBanDecisions(uuid, ip);
        OnlinePlayer online;
        synchronized (this)
        {
            online = onlinePlayers.get(uuid);
        }
        if (online != null) refreshUntilResolved(online);
    }

    private void enforceBuffer()
    {
        List<Player> victims = new ArrayList<>();
        synchronized (this)
        {
            int limit = Math.max(0, plugin.getServer().getMaxPlayers() - 1);
            int excess = onlinePlayers.size() - limit;
            while (excess-- > 0)
            {
                UUID victimId = oldestAvailableRestriction();
                if (victimId == null) break;
                evicting.add(victimId);
                OnlinePlayer victim = onlinePlayers.get(victimId);
                if (victim != null) victims.add(victim.player());
            }
        }
        victims.forEach(player -> evict(player, null));
    }

    @Nullable
    private UUID oldestAvailableRestriction()
    {
        return onlinePlayers.keySet().stream()
                .filter(uuid -> !evicting.contains(uuid))
                .filter(uuid -> !protectedFromEviction.contains(uuid))
                .filter(this::isRestricted)
                .findFirst().orElse(null);
    }

    private void evict(Player player, @Nullable Player fallback)
    {
        player.getScheduler().run(plugin, task ->
        {
            if (canEvict(player.getUniqueId()))
            {
                BungeeUtil.kickPlayer(plugin, player, PlexUtils.messageComponent("bannedPriorityKick"));
            }
            else if (fallback != null)
            {
                OnlinePlayer replacement;
                synchronized (this)
                {
                    evicting.remove(player.getUniqueId());
                    UUID replacementId = oldestAvailableRestriction();
                    if (replacementId != null) evicting.add(replacementId);
                    replacement = replacementId == null ? null : onlinePlayers.get(replacementId);
                }
                if (replacement != null)
                {
                    evict(replacement.player(), fallback);
                }
                else
                {
                    fallback.getScheduler().run(plugin, ignored ->
                            fallback.kick(Component.translatable("multiplayer.disconnect.server_full")), null);
                }
            }
        }, null);
    }

    private synchronized boolean canEvict(UUID uuid)
    {
        OnlineRestriction restriction = restrictions.get(uuid);
        return restriction != null && !protectedFromEviction.contains(uuid)
                && !removingBanOwners.containsKey(restriction.punishment.getPunished()) && isRestricted(uuid);
    }

    private void applyReservation(PlayerServerFullCheckEvent event, Reservation reservation,
                                  @Nullable PendingAdmission admission)
    {
        if (reservation.allowed())
        {
            event.allow(true);
            return;
        }
        if (admission != null && isEffective(admission.punishment()))
        {
            event.deny(Punishment.generateBanCapacityMessage(admission.punishment(),
                    plugin.config.getString("banning.ban_url")));
        }
        else
        {
            event.allow(false);
        }
    }

    private void releaseReservation(UUID uuid)
    {
        Reservation reservation = reservations.remove(uuid);
        if (reservation != null && reservation.victim() != null) evicting.remove(reservation.victim());
    }

    private static String currentIp(Player player)
    {
        return player.getAddress() == null || player.getAddress().getAddress() == null ? ""
                : BanDecisionService.canonicalIp(player.getAddress().getAddress().getHostAddress());
    }

    private static ItemStack[] cloneContents(ItemStack[] contents)
    {
        ItemStack[] clone = new ItemStack[contents.length];
        for (int index = 0; index < contents.length; index++)
        {
            clone[index] = contents[index] == null ? null : contents[index].clone();
        }
        return clone;
    }

    private static ItemStack[] inventorySnapshot(Player player, @Nullable OnlineRestriction previous)
    {
        if (previous != null && previous.inventoryContents != null) return cloneContents(previous.inventoryContents);
        return cloneContents(player.getInventory().getContents());
    }

    private static boolean isEffective(@Nullable Punishment punishment)
    {
        return punishment != null && punishment.isActive() && punishment.getEndDate() != null
                && punishment.getEndDate().isAfter(ZonedDateTime.now());
    }

    private record PendingAdmission(String ip, @Nullable Punishment punishment, long token) { }
    private record PendingPlayer(UUID uuid, String ip) { }
    private record Reservation(boolean allowed, @Nullable UUID victim) { }
    private record OnlinePlayer(Player player, String ip, long connectionToken) { }
    private record JoinPlan(@Nullable PendingAdmission admission, @Nullable OnlinePlayer victim,
                            boolean rejectIncoming, long admissionVersion) { }
    private record ActivationPlan(@Nullable OnlineRestriction previous, boolean wasRestricted, boolean shouldEvict) { }

    private static final class OnlineRestriction
    {
        private final Punishment punishment;
        private final BossBar bar;
        private ScheduledTask expiryTask;
        private OnlineRestriction previous;
        private ItemStack[] inventoryContents;

        private OnlineRestriction(Punishment punishment, BossBar bar)
        {
            this.punishment = punishment;
            this.bar = bar;
        }
    }
}
