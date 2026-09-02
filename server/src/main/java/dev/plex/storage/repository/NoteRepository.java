package dev.plex.storage.repository;

import dev.plex.api.note.PlayerNote;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.Nullable;

public interface NoteRepository
{
    CompletableFuture<List<PlayerNote>> getNotes(UUID uuid);

    CompletableFuture<Boolean> deleteNote(int id, UUID uuid);

    CompletableFuture<Void> addNote(UUID player, String content, @Nullable UUID author, ZonedDateTime timestamp);

    CompletableFuture<Integer> clearNotes(UUID uuid);
}
