package dev.plex.cache;

import com.google.common.collect.Maps;
import dev.plex.player.PlexPlayer;

import java.util.Map;
import java.util.UUID;

/**
 * Cache storage
 */

public class PlayerCache
{
    /**
     * A key/value pair where the key is the unique ID of the Plex Player
     */
    private static final Map<UUID, PlexPlayer> plexPlayerMap = Maps.newHashMap();

    public Map<UUID, PlexPlayer> getPlexPlayerMap()
    {
        return plexPlayerMap;
    }

    public PlexPlayer getPlexPlayer(UUID uuid)
    {
        return getPlexPlayerMap().get(uuid);
    }
}
