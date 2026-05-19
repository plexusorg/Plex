package dev.plex.api.player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Looks up Plex players through read-only API views.
 */
public interface PlayersApi
{
    /**
     * Looks up a player by UUID.
     *
     * @param uuid player UUID
     * @return player view, if known
     */
    Optional<? extends PlexPlayerView> byUuid(UUID uuid);

    /**
     * Looks up a player by name.
     *
     * @param name player name
     * @return player view, if known
     */
    Optional<? extends PlexPlayerView> byName(String name);

    /**
     * Returns the names of currently online players.
     *
     * @return names of currently online players
     */
    List<String> onlineNames();
}
