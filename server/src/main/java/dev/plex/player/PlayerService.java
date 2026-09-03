package dev.plex.player;

import dev.plex.storage.repository.PlayerRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

public class PlayerService
{
    private final ConcurrentMap<UUID, PlexPlayer> players = new ConcurrentHashMap<>();
    private final PlayerRepository playerRepository;
    private final Executor executor;
    private final ConcurrentHashMap<UUID, CompletableFuture<PlexPlayer>> preparedSessions = new ConcurrentHashMap<>();

    public PlayerService(PlayerRepository playerRepository, Executor executor)
    {
        this.playerRepository = playerRepository;
        this.executor = executor;
    }

    public CompletableFuture<Boolean> playerExists(UUID uuid)
    {
        return read(() -> playerRepository.exists(uuid));
    }

    public CompletableFuture<Boolean> playerExists(String username)
    {
        return read(() -> playerRepository.exists(username));
    }

    public CompletableFuture<PlexPlayer> findPlayer(UUID uuid)
    {
        PlexPlayer cached = cachedPlayer(uuid);
        return cached == null
                ? read(() -> playerRepository.getByUUID(uuid, true))
                : CompletableFuture.completedFuture(cached);
    }

    public CompletableFuture<PlexPlayer> findPlayer(String username)
    {
        Optional<PlexPlayer> cached = cachedPlayers().stream()
                .filter(player -> player.getName().equalsIgnoreCase(username))
                .findFirst();
        return cached.isPresent()
                ? CompletableFuture.completedFuture(cached.get())
                : read(() -> playerRepository.getByName(username, true));
    }

    public CompletableFuture<String> findName(UUID uuid)
    {
        PlexPlayer cached = cachedPlayer(uuid);
        return cached == null
                ? read(() -> playerRepository.getNameByUUID(uuid))
                : CompletableFuture.completedFuture(cached.getName());
    }

    public CompletableFuture<PlexPlayer> findPlayerByIp(String ip)
    {
        String canonicalIp = dev.plex.punishment.admission.BanDecisionService.canonicalIp(ip);
        PlexPlayer player = cachedPlayers().stream()
                .filter(plexPlayer -> plexPlayer.getIps().contains(canonicalIp)).findFirst().orElse(null);
        if (player != null)
        {
            return CompletableFuture.completedFuture(player);
        }
        return read(() -> playerRepository.getByIP(canonicalIp));
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

    public PlexPlayer cachedPlayer(UUID uuid) { return players.get(uuid); }
    public boolean isCached(UUID uuid) { return players.containsKey(uuid); }
    public void cache(PlexPlayer player) { players.put(player.getUuid(), player); }
    public Collection<PlexPlayer> cachedPlayers() { return List.copyOf(players.values()); }

    public CompletableFuture<PlexPlayer> prepareSession(UUID uuid, String username, String ip)
    {
        String normalizedIp = dev.plex.punishment.admission.BanDecisionService.canonicalIp(ip);
        CompletableFuture<PlexPlayer> pending = new CompletableFuture<>();
        CompletableFuture<PlexPlayer> existing = preparedSessions.putIfAbsent(uuid, pending);
        if (existing != null) return existing;
        try
        {
            CompletableFuture.supplyAsync(() -> loadSession(uuid, username, normalizedIp), executor)
                    .whenComplete((player, failure) ->
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

    public CompletableFuture<PlexPlayer> reloadSession(UUID uuid, String username, String ip)
    {
        String normalizedIp = dev.plex.punishment.admission.BanDecisionService.canonicalIp(ip);
        return read(() -> loadSession(uuid, username, normalizedIp));
    }

    public boolean attachReloadedSession(UUID uuid, PlexPlayer expected, PlexPlayer reloaded)
    {
        return expected == null
                ? players.putIfAbsent(uuid, reloaded) == null
                : players.replace(uuid, expected, reloaded);
    }

    private PlexPlayer loadSession(UUID uuid, String username, String normalizedIp)
    {
        PlexPlayer player = playerRepository.getByUUID(uuid, true);
        if (player == null)
        {
            player = new PlexPlayer(uuid);
            player.setName(username);
            if (!normalizedIp.isEmpty()) player.getIps().add(normalizedIp);
            playerRepository.insert(player);
            return player;
        }
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
        if (changed) playerRepository.update(player);
        return player;
    }

    public PlexPlayer attachPreparedSession(UUID uuid)
    {
        CompletableFuture<PlexPlayer> pending = preparedSessions.remove(uuid);
        PlexPlayer player = pending == null ? null : pending.getNow(null);
        if (player != null) cache(player);
        return player;
    }

    public CompletableFuture<Void> detachAndSave(UUID uuid)
    {
        PlexPlayer player = players.remove(uuid);
        return player == null ? CompletableFuture.completedFuture(null) : update(player);
    }

    public CompletableFuture<Void> flush()
    {
        CompletableFuture<?>[] operations = cachedPlayers().stream()
                .map(this::update).toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(operations);
    }
}
