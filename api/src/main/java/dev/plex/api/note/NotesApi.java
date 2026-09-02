package dev.plex.api.note;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.Nullable;

/**
 * Manages notes attached to Plex players.
 */
public interface NotesApi
{
    /**
     * Returns a player's notes in ID order.
     *
     * @param player player UUID
     * @return future containing the player's notes
     */
    CompletableFuture<List<PlayerNote>> list(UUID player);

    /**
     * Adds a note to a player.
     *
     * @param player player UUID
     * @param content note content
     * @param author author UUID, or {@code null} for console
     * @return future completed after the note is stored
     */
    CompletableFuture<Void> add(UUID player, String content, @Nullable UUID author);

    /**
     * Removes a note from a player.
     *
     * @param player player UUID
     * @param id player-local note ID
     * @return future containing whether the note existed
     */
    CompletableFuture<Boolean> remove(UUID player, int id);

    /**
     * Removes every note from a player.
     *
     * @param player player UUID
     * @return future containing the number of removed notes
     */
    CompletableFuture<Integer> clear(UUID player);
}
