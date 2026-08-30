package dev.plex.api.player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import dev.plex.module.PlexModule;

/**
 * Looks up Plex players through read-only API views.
 */
public interface PlayersApi
{
    /**
     * Looks up a player by UUID.
     *
     * @param uuid player UUID
     * @return future containing the player view, if known
     */
    CompletableFuture<Optional<PlexPlayerView>> player(UUID uuid);

    /**
     * Looks up a player by name.
     *
     * @param name player name
     * @return future containing the player view, if known
     */
    CompletableFuture<Optional<PlexPlayerView>> byName(String name);

    /**
     * Returns the names of online players.
     *
     * @return names of online players
     */
    List<String> onlineNames();

    /**
     * Returns module-scoped data storage for a player.
     *
     * @param module module requesting player data
     * @param playerUuid player UUID
     * @return module-scoped player data storage
     */
    PlayerModuleData moduleData(PlexModule module, UUID playerUuid);
}
