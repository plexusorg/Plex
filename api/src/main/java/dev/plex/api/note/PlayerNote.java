package dev.plex.api.note;

import java.time.ZonedDateTime;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * A note attached to a Plex player.
 *
 * @param id player-local note ID
 * @param player UUID of the player the note belongs to
 * @param content note content
 * @param author UUID of the author, or {@code null} for console
 * @param timestamp date and time the note was written
 */
public record PlayerNote(int id, UUID player, String content, @Nullable UUID author, ZonedDateTime timestamp)
{
}
