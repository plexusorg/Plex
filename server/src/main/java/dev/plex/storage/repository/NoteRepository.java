package dev.plex.storage.repository;

import dev.plex.punishment.extra.Note;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface NoteRepository
{
    CompletableFuture<List<Note>> getNotes(UUID uuid);

    CompletableFuture<Void> deleteNote(int id, UUID uuid);

    CompletableFuture<Void> addNote(Note note);
}
