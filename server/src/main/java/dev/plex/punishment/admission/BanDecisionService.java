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

public final class BanDecisionService
{
    private final PunishmentRepository repository;
    private final Cache<Key, CompletableFuture<Optional<Punishment>>> cache;

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
        try
        {
            CompletableFuture<Optional<Punishment>> future = cache.get(key, () -> load(key));
            return future.thenCompose(result ->
            {
                boolean expired = result.isPresent() && result.get().getEndDate() != null
                        && !result.get().getEndDate().toInstant().isAfter(Instant.now());
                if (!expired)
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

    private CompletableFuture<Optional<Punishment>> load(Key key)
    {
        CompletableFuture<Optional<Punishment>> future = repository.getEffectiveBan(key.uuid(), key.ip(), Instant.now());
        future.whenComplete((result, failure) ->
        {
            if (failure != null) cache.asMap().remove(key, future);
        });
        return future;
    }

    public void invalidate(UUID uuid, String ip)
    {
        String canonicalIp = ip == null ? null : canonicalIp(ip);
        cache.asMap().keySet().removeIf(key -> key.uuid().equals(uuid)
                || canonicalIp != null && key.ip().equals(canonicalIp));
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
}
