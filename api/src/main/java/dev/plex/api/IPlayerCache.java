package dev.plex.api;

import java.util.Map;
import java.util.UUID;

public interface IPlayerCache<T>
{
    Map<UUID, T> getPlexPlayerMap();

    T getPlexPlayer(UUID uuid);
}
