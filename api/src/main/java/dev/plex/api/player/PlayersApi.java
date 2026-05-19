package dev.plex.api.player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlayersApi
{
    Optional<? extends PlexPlayerView> byUuid(UUID uuid);
    Optional<? extends PlexPlayerView> byName(String name);
    List<String> onlineNames();
}
