package dev.plex.api.player;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import dev.plex.module.PlexModule;
import net.kyori.adventure.text.Component;

/**
 * Looks up Plex players and manages their saved custom tags.
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
     * Saves a player's custom tag for chat and the tab list.
     *
     * <p>Works for known online and offline players. Applies Plex's visual formatting
     * rules and configured {@code chat.max-tag-length}. This does not set a rank
     * prefix or an additive tab prefix. The caller must enforce its own permissions.</p>
     *
     * <p>Supports text, sprites, and player heads. Removes click and hover actions,
     * insertion text, obfuscated formatting, and right-to-left text.
     * Rejects dynamic component types such as translations, scores, and selectors.</p>
     *
     * <p>The future completes after persistence and the local cached tag update.
     * It fails for an unknown UUID, an invalid tag, or a storage failure. An overlong
     * tag fails with {@link TagTooLongException}. No player
     * record is created. Callbacks may run on a database thread; do not block a
     * player or region thread waiting for completion.</p>
     *
     * @param uuid player UUID
     * @param tag custom tag component
     * @return completion of the saved tag change
     */
    CompletableFuture<Void> setTag(UUID uuid, Component tag);

    /**
     * Removes a known player's saved custom tag from chat and the tab list.
     *
     * <p>Uses the same persistence and completion contract as {@link #setTag}.
     * Rank fallback and additive tab prefixes remain unchanged.</p>
     *
     * @param uuid player UUID
     * @return completion of the saved tag removal
     */
    CompletableFuture<Void> clearTag(UUID uuid);

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
