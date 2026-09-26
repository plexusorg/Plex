package dev.plex.punishment.admission;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.net.InetAddresses;
import dev.plex.punishment.Punishment;
import dev.plex.punishment.BanIpRange;
import dev.plex.punishment.TargetBan;
import dev.plex.storage.repository.PunishmentRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public final class BanDecisionService
{
    private final PunishmentRepository repository;
    private final Cache<Key, CompletableFuture<Optional<Punishment>>> cache;
    private static final int REVISION_STRIPES = 256;
    private final AtomicLong[] uuidRevisions = revisions();
    private final AtomicLong[] ipRevisions = revisions();
    private List<TargetBan> targetBans = List.of();
    private final AtomicLong targetRevision = new AtomicLong();
    private CompletableFuture<Void> targetReload;
    private CompletableFuture<Void> targetMutations = CompletableFuture.completedFuture(null);

    public BanDecisionService(PunishmentRepository repository, Duration ttl, int maximumSize)
    {
        this.repository = repository;
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(ttl)
                .build();
    }

    public synchronized CompletableFuture<Void> reloadTargetBans()
    {
        if (targetReload != null && !targetReload.isDone()) return targetReload;
        targetReload = loadTargetBans();
        return targetReload;
    }

    private synchronized CompletableFuture<Void> loadTargetBans()
    {
        long revision = targetRevision.get();
        return repository.loadActiveTargetBans(Instant.now()).thenCompose(bans ->
        {
            synchronized (this)
            {
                if (targetRevision.get() != revision) return loadTargetBans();
                targetBans = List.copyOf(bans);
                targetRevision.incrementAndGet();
                return CompletableFuture.completedFuture(null);
            }
        });
    }

    public synchronized CompletableFuture<Boolean> mutateTargetBans(Supplier<CompletableFuture<Boolean>> mutation)
    {
        CompletableFuture<Boolean> result = targetMutations.thenCompose(unused -> mutation.get());
        targetMutations = result.handle((changed, failure) -> null);
        return result;
    }

    public synchronized void addTargetBan(TargetBan ban)
    {
        List<TargetBan> updated = new ArrayList<>(targetBans);
        updated.removeIf(existing -> !existing.endAt().isAfter(Instant.now()));
        updated.add(ban);
        targetBans = List.copyOf(updated);
        targetRevision.incrementAndGet();
    }

    public synchronized void removeTargetBan(TargetBan.Kind kind, String target)
    {
        targetBans = targetBans.stream().filter(ban -> ban.kind() != kind || !ban.target().equals(target))
                .filter(ban -> ban.endAt().isAfter(Instant.now())).toList();
        targetRevision.incrementAndGet();
    }

    public synchronized Optional<Punishment> targetBan(UUID uuid, String username, String ip)
    {
        Instant now = Instant.now();
        if (targetBans.stream().anyMatch(ban -> !ban.endAt().isAfter(now)))
        {
            targetBans = targetBans.stream().filter(ban -> ban.endAt().isAfter(now)).toList();
            targetRevision.incrementAndGet();
        }
        if (targetBans.isEmpty()) return Optional.empty();
        String key = BanIpRange.banMatchKey(ip);
        BanIpRange address = key.isEmpty() ? null : BanIpRange.parse(key);
        return targetBans.stream().filter(ban -> ban.kind() == TargetBan.Kind.NAME
                ? ban.target().equalsIgnoreCase(username)
                : address != null && ban.range().contains(address))
                .max((first, second) -> first.endAt().compareTo(second.endAt()))
                .map(ban -> ban.asPunishment(uuid));
    }

    public CompletableFuture<Optional<Punishment>> decide(UUID uuid, String ip)
    {
        Key key = new Key(uuid, BanIpRange.banMatchKey(ip));
        Revision observedRevision = revision(uuid, key.ip());
        try
        {
            CompletableFuture<Optional<Punishment>> future = cache.get(key,
                    () -> repository.getEffectiveBan(key.uuid(), key.ip(), Instant.now()));
            return future.whenComplete((result, failure) ->
            {
                if (failure != null) cache.asMap().remove(key, future);
            }).thenCompose(result ->
            {
                if (!revision(key.uuid(), key.ip()).equals(observedRevision))
                {
                    cache.asMap().remove(key, future);
                    return decide(key.uuid(), key.ip());
                }
                boolean stale = result.isPresent() && (!result.get().isActive()
                        || result.get().getEndDate() != null
                        && !result.get().getEndDate().toInstant().isAfter(Instant.now()));
                if (!stale)
                {
                    return CompletableFuture.completedFuture(result);
                }

                // The database may contain another overlapping UUID/IP ban. Once the
                // cached winner expires, reload instead of treating the player as clear
                // until the cache TTL elapses.
                cache.asMap().remove(key, future);
                return decide(key.uuid(), key.ip());
            });
        }
        catch (ExecutionException failure)
        {
            return CompletableFuture.failedFuture(failure.getCause());
        }
    }

    public void invalidate(UUID uuid, String ip)
    {
        String canonicalIp = ip == null ? null : BanIpRange.banMatchKey(ip);
        uuidRevisions[stripe(uuid)].incrementAndGet();
        if (canonicalIp != null) ipRevisions[stripe(canonicalIp)].incrementAndGet();
        cache.asMap().keySet().removeIf(key -> key.uuid().equals(uuid)
                || canonicalIp != null && key.ip().equals(canonicalIp));
    }

    public Revision revision(UUID uuid, String ip)
    {
        return new Revision(uuidRevisions[stripe(uuid)].get(), ipRevisions[stripe(BanIpRange.banMatchKey(ip))].get(), targetRevision.get());
    }

    private static AtomicLong[] revisions()
    {
        AtomicLong[] revisions = new AtomicLong[REVISION_STRIPES];
        Arrays.setAll(revisions, ignored -> new AtomicLong());
        return revisions;
    }

    private static int stripe(Object value)
    {
        return (value.hashCode() & Integer.MAX_VALUE) % REVISION_STRIPES;
    }

    public static String canonicalIp(String ip)
    {
        if (ip == null) return "";
        String value = ip.trim().toLowerCase(Locale.ROOT);
        if (value.length() > 1 && value.charAt(0) == '[' && value.charAt(value.length() - 1) == ']')
        {
            value = value.substring(1, value.length() - 1);
        }
        return InetAddresses.isInetAddress(value) ? InetAddresses.toAddrString(InetAddresses.forString(value)) : value;
    }

    private record Key(UUID uuid, String ip) { }

    public record Revision(long uuid, long ip, long targets) { }
}
