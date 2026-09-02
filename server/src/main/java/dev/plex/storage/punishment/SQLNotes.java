package dev.plex.storage.punishment;

import dev.plex.api.note.PlayerNote;
import dev.plex.storage.database.entity.NoteEntity;
import dev.plex.storage.repository.NoteRepository;
import dev.plex.util.TimeUtils;
import org.jdbi.v3.core.Jdbi;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.jetbrains.annotations.Nullable;

public class SQLNotes implements NoteRepository
{
    private final Object insertLock = new Object();
    private final Jdbi jdbi;
    private final Executor executor;

    public SQLNotes(Jdbi jdbi, Executor executor)
    {
        this.jdbi = jdbi;
        this.executor = executor;
    }

    public CompletableFuture<List<PlayerNote>> getNotes(UUID uuid)
    {
        return CompletableFuture.supplyAsync(() -> jdbi.withHandle(h -> h.createQuery("SELECT * FROM notes WHERE uuid = :u")
                        .bind("u", uuid.toString()).map((rs, ctx) -> mapRow(rs)).list()).stream()
                        .sorted(Comparator.comparingInt(NoteEntity::getId))
                        .map(this::toNote)
                        .flatMap(Optional::stream)
                    .toList(), executor);
    }

    public CompletableFuture<Boolean> deleteNote(int id, UUID uuid)
    {
        return CompletableFuture.supplyAsync(() -> jdbi.withHandle(h -> h.createUpdate("DELETE FROM notes WHERE uuid = :u AND id = :id")
                        .bind("u", uuid.toString())
                        .bind("id", id)
                        .execute()) > 0, executor);
    }

    public CompletableFuture<Void> addNote(UUID player, String content, @Nullable UUID author, ZonedDateTime timestamp)
    {
        return CompletableFuture.runAsync(() ->
        {
            synchronized (insertLock)
            {
                jdbi.useTransaction(h ->
                {
                    int nextId = h.createQuery("SELECT COALESCE(MAX(id), 0) FROM notes WHERE uuid = :u")
                            .bind("u", player.toString()).mapTo(Integer.class).one() + 1;
                    NoteEntity entity = toEntity(player, content, author, timestamp);
                    entity.setId(nextId);
                    h.createUpdate(
                                "INSERT INTO notes (id, uuid, written_by_uuid, note, timestamp) " +
                                        "VALUES (:id, :uuid, :writtenBy, :note, :ts)")
                            .bind("id", entity.getId())
                            .bind("uuid", entity.getUuid())
                            .bind("writtenBy", entity.getWrittenByUuid())
                            .bind("note", entity.getNote())
                            .bind("ts", entity.getTimestamp())
                            .execute();
                });
            }
        }, executor);
    }

    @Override
    public CompletableFuture<Integer> clearNotes(UUID uuid)
    {
        return CompletableFuture.supplyAsync(() -> jdbi.withHandle(h -> h.createUpdate("DELETE FROM notes WHERE uuid = :u")
                .bind("u", uuid.toString()).execute()), executor);
    }

    private static NoteEntity mapRow(java.sql.ResultSet rs) throws java.sql.SQLException
    {
        NoteEntity e = new NoteEntity();
        e.setRowId(rs.getLong("row_id"));
        e.setId(rs.getInt("id"));
        e.setUuid(rs.getString("uuid"));
        e.setWrittenByUuid(rs.getString("written_by_uuid"));
        e.setNote(rs.getString("note"));
        e.setTimestamp(rs.getLong("timestamp"));
        return e;
    }

    private Optional<PlayerNote> toNote(NoteEntity entity)
    {
        try
        {
            String writtenBy = entity.getWrittenByUuid();
            return Optional.of(new PlayerNote(entity.getId(), UUID.fromString(entity.getUuid()), entity.getNote(),
                    writtenBy == null ? null : UUID.fromString(writtenBy),
                    ZonedDateTime.ofInstant(Instant.ofEpochMilli(entity.getTimestamp()), TimeUtils.zoneId())));
        }
        catch (IllegalArgumentException | NullPointerException e)
        {
            return Optional.empty();
        }
    }

    private NoteEntity toEntity(UUID player, String content, @Nullable UUID author, ZonedDateTime timestamp)
    {
        NoteEntity entity = new NoteEntity();
        entity.setUuid(player.toString());
        entity.setWrittenByUuid(author == null ? null : author.toString());
        entity.setNote(content);
        entity.setTimestamp(timestamp.toInstant().toEpochMilli());
        return entity;
    }
}
