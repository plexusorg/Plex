package dev.plex.player;

import dev.plex.api.player.TagTooLongException;
import dev.plex.punishment.admission.BanDecisionService;
import dev.plex.storage.repository.PlayerRepository;
import dev.plex.util.minimessage.SafeMiniMessage;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.ObjectComponent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

public class PlayerService
{
    private final ConcurrentMap<UUID, PlexPlayer> players = new ConcurrentHashMap<>();
    private final PlayerRepository playerRepository;
    private final Executor executor;
    private final IntSupplier maximumTagLength;
    // Serialize tag writes and session reads without holding a lock during database work.
    private final ConcurrentMap<UUID, CompletableFuture<?>> operations = new ConcurrentHashMap<>();
    private final ConcurrentMap<UUID, CompletableFuture<PlexPlayer>> reloadedSessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<UUID, CompletableFuture<PlexPlayer>> preparedSessions = new ConcurrentHashMap<>();

    public PlayerService(PlayerRepository playerRepository, Executor executor, IntSupplier maximumTagLength)
    {
        this.playerRepository = playerRepository;
        this.executor = executor;
        this.maximumTagLength = maximumTagLength;
    }

    public CompletableFuture<Void> setTag(UUID uuid, Component tag)
    {
        try
        {
            Objects.requireNonNull(uuid, "uuid");
            Component safe = visualTag(Objects.requireNonNull(tag, "tag"));
            int maximum = maximumTagLength.getAsInt();
            if (PlainTextComponentSerializer.plainText().serialize(safe).length() > maximum)
            {
                throw new TagTooLongException(maximum);
            }
            return persistTag(uuid, SafeMiniMessage.mmSerialize(safe));
        }
        catch (RuntimeException failure)
        {
            return CompletableFuture.failedFuture(failure);
        }
    }

    public CompletableFuture<Void> clearTag(UUID uuid)
    {
        try
        {
            return persistTag(Objects.requireNonNull(uuid, "uuid"), null);
        }
        catch (RuntimeException failure)
        {
            return CompletableFuture.failedFuture(failure);
        }
    }

    private Component visualTag(Component tag)
    {
        if (!(tag instanceof TextComponent) && !(tag instanceof ObjectComponent))
        {
            throw new IllegalArgumentException("Tags support text, sprites, and player heads only");
        }
        if (tag instanceof TextComponent text)
        {
            tag = text.content(PlexUtils.cleanString(text.content()));
        }
        return tag.clickEvent(null).hoverEvent(null).insertion(null)
                .decoration(TextDecoration.OBFUSCATED, TextDecoration.State.FALSE)
                .children(tag.children().stream().map(this::visualTag).toList());
    }

    private CompletableFuture<Void> persistTag(UUID uuid, String tag)
    {
        return ordered(uuid, () ->
        {
            playerRepository.setTag(uuid, tag);
            synchronized (players)
            {
                PlexPlayer cached = players.get(uuid);
                if (cached != null) cached.setPrefix(tag);
                CompletableFuture<PlexPlayer> prepared = preparedSessions.get(uuid);
                if (prepared != null && prepared.isDone() && !prepared.isCompletedExceptionally())
                {
                    prepared.join().setPrefix(tag);
                }
                CompletableFuture<PlexPlayer> reloaded = reloadedSessions.get(uuid);
                if (reloaded != null && reloaded.isDone() && !reloaded.isCompletedExceptionally())
                {
                    reloaded.join().setPrefix(tag);
                }
            }
            return null;
        });
    }

    private <T> CompletableFuture<T> ordered(UUID uuid, Supplier<T> action)
    {
        CompletableFuture<T> next;
        synchronized (operations)
        {
            CompletableFuture<?> previous = operations.getOrDefault(uuid, CompletableFuture.completedFuture(null));
            next = previous.handle((ignored, failure) -> null).thenApplyAsync(ignored -> action.get(), executor);
            operations.put(uuid, next);
        }
        next.whenComplete((ignored, failure) -> operations.remove(uuid, next));
        return next.copy();
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
        String canonicalIp = BanDecisionService.canonicalIp(ip);
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
        String normalizedIp = BanDecisionService.canonicalIp(ip);
        CompletableFuture<PlexPlayer> pending = new CompletableFuture<>();
        CompletableFuture<PlexPlayer> existing = preparedSessions.putIfAbsent(uuid, pending);
        if (existing != null) return existing;
        try
        {
            ordered(uuid, () ->
            {
                PlexPlayer player = loadSession(uuid, username, normalizedIp);
                pending.complete(player);
                return player;
            }).whenComplete((player, failure) ->
            {
                if (failure != null)
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
        String normalizedIp = BanDecisionService.canonicalIp(ip);
        CompletableFuture<PlexPlayer> pending = new CompletableFuture<>();
        reloadedSessions.put(uuid, pending);
        try
        {
            ordered(uuid, () ->
            {
                PlexPlayer player = loadSession(uuid, username, normalizedIp);
                pending.complete(player);
                return player;
            }).whenComplete((player, failure) ->
            {
                if (failure != null)
                {
                    pending.completeExceptionally(failure);
                    reloadedSessions.remove(uuid, pending);
                }
            });
        }
        catch (RuntimeException failure)
        {
            reloadedSessions.remove(uuid, pending);
            pending.completeExceptionally(failure);
        }
        CompletableFuture.delayedExecutor(2, TimeUnit.MINUTES).execute(() -> reloadedSessions.remove(uuid, pending));
        return pending;
    }

    public boolean attachReloadedSession(UUID uuid, PlexPlayer expected, PlexPlayer reloaded)
    {
        synchronized (players)
        {
            CompletableFuture<PlexPlayer> pending = reloadedSessions.get(uuid);
            if (pending == null || pending.getNow(null) != reloaded) return false;
            reloadedSessions.remove(uuid, pending);
            return expected == null
                    ? players.putIfAbsent(uuid, reloaded) == null
                    : players.replace(uuid, expected, reloaded);
        }
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
        synchronized (players)
        {
            CompletableFuture<PlexPlayer> pending = preparedSessions.remove(uuid);
            PlexPlayer player = pending == null ? null : pending.getNow(null);
            if (player != null) cache(player);
            return player;
        }
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
