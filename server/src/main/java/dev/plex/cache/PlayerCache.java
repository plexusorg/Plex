package dev.plex.cache;

import dev.plex.player.PlexPlayer;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class PlayerCache
{
    private final ConcurrentMap<UUID, PlexPlayer> players = new ConcurrentHashMap<>();

    public PlexPlayer getPlexPlayer(UUID uuid)
    {
        return players.get(uuid);
    }

    public void put(PlexPlayer player)
    {
        players.put(player.getUuid(), player);
    }

    public PlexPlayer remove(UUID uuid) { return players.remove(uuid); }
    public boolean contains(UUID uuid) { return players.containsKey(uuid); }
    public Collection<PlexPlayer> snapshot() { return List.copyOf(players.values()); }
}
