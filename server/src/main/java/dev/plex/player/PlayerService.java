package dev.plex.player;

import dev.plex.cache.PlayerCache;
import dev.plex.storage.repository.PlayerRepository;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class PlayerService
{
    private final PlayerCache playerCache;
    private final PlayerRepository playerRepository;
    private final Executor executor;
    private final ConcurrentHashMap<UUID, CompletableFuture<PlexPlayer>> preparedSessions = new ConcurrentHashMap<>();

    public PlayerService(PlayerCache playerCache, PlayerRepository playerRepository, Executor executor)
    {
        this.playerCache = playerCache;
        this.playerRepository = playerRepository;
        this.executor = executor;
    }

    public boolean hasPlayedBefore(UUID uuid)
    {
        return playerRepository.exists(uuid);
    }

    public boolean hasPlayedBefore(String username)
    {
        return playerRepository.exists(username);
    }

    public PlexPlayer getPlayer(UUID uuid)
    {
        return getPlayer(uuid, true);
    }

    public PlexPlayer getPlayer(UUID uuid, boolean loadExtraData)
    {
        PlexPlayer cached = playerCache.getPlexPlayer(uuid);
        if (cached != null) return cached;

        return playerRepository.getByUUID(uuid, loadExtraData);
    }

    public PlexPlayer getPlayer(String username)
    {
        return getPlayer(username, true);
    }

    public PlexPlayer getPlayer(String username, boolean loadExtraData)
    {
        Optional<PlexPlayer> plexPlayer = playerCache.snapshot().stream().filter(player -> player.getName().equalsIgnoreCase(username)).findFirst();
        return plexPlayer.orElseGet(() -> playerRepository.getByName(username, loadExtraData));
    }

    public CompletableFuture<PlexPlayer> findPlayer(UUID uuid)
    {
        PlexPlayer cached = playerCache.getPlexPlayer(uuid);
        return cached == null
                ? read(() -> playerRepository.getByUUID(uuid, true))
                : CompletableFuture.completedFuture(cached);
    }

    public CompletableFuture<PlexPlayer> findPlayer(String username)
    {
        Optional<PlexPlayer> cached = playerCache.snapshot().stream()
                .filter(player -> player.getName().equalsIgnoreCase(username))
                .findFirst();
        return cached.isPresent()
                ? CompletableFuture.completedFuture(cached.get())
                : read(() -> playerRepository.getByName(username, true));
    }

    public CompletableFuture<String> findName(UUID uuid)
    {
        PlexPlayer cached = playerCache.getPlexPlayer(uuid);
        return cached == null
                ? read(() -> playerRepository.getNameByUUID(uuid))
                : CompletableFuture.completedFuture(cached.getName());
    }

    public PlexPlayer getPlayerByIP(String ip)
    {
        PlexPlayer player = playerCache.snapshot().stream().filter(plexPlayer -> plexPlayer.getIps().contains(ip)).findFirst().orElse(null);
        if (player != null)
        {
            return player;
        }

        return playerRepository.getByIP(ip);
    }

    public String getNameByUUID(UUID uuid)
    {
        PlexPlayer player = playerCache.getPlexPlayer(uuid);
        if (player != null)
        {
            return player.getName();
        }
        return playerRepository.getNameByUUID(uuid);
    }

    public CompletableFuture<Void> update(PlexPlayer plexPlayer)
    {
        return write(() -> playerRepository.update(plexPlayer));
    }

    public CompletableFuture<Void> insert(PlexPlayer plexPlayer)
    {
        return write(() -> playerRepository.insert(plexPlayer));
    }

    private CompletableFuture<Void> write(Runnable action)
    {
        try
        {
            return CompletableFuture.runAsync(action, executor);
        }
        catch (RuntimeException failure)
        {
            return CompletableFuture.failedFuture(failure);
        }
    }

    private <T> CompletableFuture<T> read(java.util.function.Supplier<T> action)
    {
        try
        {
            return CompletableFuture.supplyAsync(action, executor);
        }
        catch (RuntimeException failure)
        {
            return CompletableFuture.failedFuture(failure);
        }
    }

    public PlexPlayer getCachedPlayer(UUID uuid) { return playerCache.getPlexPlayer(uuid); }
    public boolean isCached(UUID uuid) { return playerCache.contains(uuid); }
    public void cache(PlexPlayer player) { playerCache.put(player); }

    public CompletableFuture<PlexPlayer> prepareSession(UUID uuid, String username, String ip)
    {
        String normalizedIp = dev.plex.punishment.admission.BanDecisionService.canonicalIp(ip);
        CompletableFuture<PlexPlayer> pending = new CompletableFuture<>();
        CompletableFuture<PlexPlayer> existing = preparedSessions.putIfAbsent(uuid, pending);
        if (existing != null) return existing;
        try
        {
            CompletableFuture.supplyAsync(() ->
            {
                PlexPlayer player = playerRepository.getByUUID(uuid, true);
                if (player == null)
                {
                    player = new PlexPlayer(uuid);
                    player.setName(username);
                    if (!normalizedIp.isEmpty())
                    {
                        player.getIps().add(normalizedIp);
                    }
                    playerRepository.insert(player);
                }
                else
                {
                    boolean changed = false;
                    if (!normalizedIp.isEmpty() && !player.getIps().contains(normalizedIp))
                    {
                        player.getIps().add(normalizedIp);
                        changed = true;
                    }
                    if (!player.getName().equals(username))
                    {
                        player.setName(username);
                        changed = true;
                    }
                    if (changed)
                    {
                        playerRepository.update(player);
                    }
                }
                return player;
            }, executor).whenComplete((player, failure) ->
            {
                if (failure == null)
                {
                    pending.complete(player);
                }
                else
                {
                    pending.completeExceptionally(failure);
                    preparedSessions.remove(uuid, pending);
                }
            });
        }
        catch (RuntimeException failure)
        {
            preparedSessions.remove(uuid, pending);
            pending.completeExceptionally(failure);
        }
        CompletableFuture.delayedExecutor(2, TimeUnit.MINUTES).execute(() -> preparedSessions.remove(uuid, pending));
        return pending;
    }

    public PlexPlayer attachPreparedSession(UUID uuid)
    {
        CompletableFuture<PlexPlayer> pending = preparedSessions.remove(uuid);
        PlexPlayer player = pending == null ? null : pending.getNow(null);
        if (player != null) playerCache.put(player);
        return player;
    }

    public CompletableFuture<Void> detachAndSave(UUID uuid)
    {
        PlexPlayer player = playerCache.remove(uuid);
        return player == null ? CompletableFuture.completedFuture(null) : update(player);
    }

    public CompletableFuture<Void> flush()
    {
        CompletableFuture<?>[] operations = playerCache.snapshot().stream()
                .map(this::update).toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(operations);
    }
}
