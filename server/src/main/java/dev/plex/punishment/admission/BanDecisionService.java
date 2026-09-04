package dev.plex.punishment.admission;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.net.InetAddresses;
import dev.plex.punishment.Punishment;
import dev.plex.storage.repository.PunishmentRepository;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicLong;

public final class BanDecisionService
{
    private final PunishmentRepository repository;
    private final Cache<Key, CompletableFuture<Optional<Punishment>>> cache;
    private static final int REVISION_STRIPES = 256;
    private final AtomicLong[] uuidRevisions = revisions();
    private final AtomicLong[] ipRevisions = revisions();

    public BanDecisionService(PunishmentRepository repository, Duration ttl, int maximumSize)
    {
        this.repository = repository;
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(maximumSize)
                .expireAfterWrite(ttl)
                .build();
    }

    public CompletableFuture<Optional<Punishment>> decide(UUID uuid, String ip)
    {
        Key key = new Key(uuid, canonicalIp(ip));
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
        String canonicalIp = ip == null ? null : canonicalIp(ip);
        uuidRevisions[stripe(uuid)].incrementAndGet();
        if (canonicalIp != null) ipRevisions[stripe(canonicalIp)].incrementAndGet();
        cache.asMap().keySet().removeIf(key -> key.uuid().equals(uuid)
                || canonicalIp != null && key.ip().equals(canonicalIp));
    }

    public Revision revision(UUID uuid, String ip)
    {
        return new Revision(uuidRevisions[stripe(uuid)].get(), ipRevisions[stripe(canonicalIp(ip))].get());
    }

    private static AtomicLong[] revisions()
    {
        AtomicLong[] revisions = new AtomicLong[REVISION_STRIPES];
        java.util.Arrays.setAll(revisions, ignored -> new AtomicLong());
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

    public record Revision(long uuid, long ip) { }
}
