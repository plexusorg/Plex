package dev.plex.api.impl;

import dev.plex.Plex;
import dev.plex.api.note.NotesApi;
import dev.plex.api.note.PlayerNote;
import dev.plex.util.TimeUtils;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.Nullable;

final class DefaultNotesApi implements NotesApi
{
    private final Plex plugin;

    DefaultNotesApi(Plex plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public CompletableFuture<List<PlayerNote>> list(UUID player)
    {
        return plugin.getNoteRepository().getNotes(Objects.requireNonNull(player, "player"));
    }

    @Override
    public CompletableFuture<Void> add(UUID player, String content, @Nullable UUID author)
    {
        Objects.requireNonNull(player, "player");
        Objects.requireNonNull(content, "content");
        if (content.length() > 2000)
        {
            return CompletableFuture.failedFuture(new IllegalArgumentException("note content must not exceed 2000 characters"));
        }
        return plugin.getNoteRepository().addNote(player, content, author, ZonedDateTime.now(TimeUtils.zoneId()));
    }

    @Override
    public CompletableFuture<Boolean> remove(UUID player, int id)
    {
        return plugin.getNoteRepository().deleteNote(id, Objects.requireNonNull(player, "player"));
    }

    @Override
    public CompletableFuture<Integer> clear(UUID player)
    {
        return plugin.getNoteRepository().clearNotes(Objects.requireNonNull(player, "player"));
    }
}
