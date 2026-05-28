package dev.plex.storage.punishment;

import com.google.common.collect.Lists;
import dev.plex.punishment.extra.Note;
import dev.plex.storage.database.entity.NoteEntity;
import dev.plex.storage.repository.NoteRepository;
import dev.plex.util.PlexLog;
import dev.plex.util.TimeUtils;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.JdbiException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class SQLNotes implements NoteRepository
{
    private final Jdbi jdbi;
    private final Executor executor;

    public SQLNotes(Jdbi jdbi, Executor executor)
    {
        this.jdbi = jdbi;
        this.executor = executor;
    }

    public CompletableFuture<List<Note>> getNotes(UUID uuid)
    {
        return CompletableFuture.supplyAsync(() ->
        {
            try
            {
                return jdbi.withHandle(h -> h.createQuery("SELECT * FROM notes WHERE uuid = :u")
                        .bind("u", uuid.toString()).map((rs, ctx) -> mapRow(rs)).list()).stream()
                        .sorted(Comparator.comparingInt(NoteEntity::getId))
                        .map(this::toNote)
                        .flatMap(Optional::stream)
                        .toList();
            }
            catch (JdbiException e)
            {
                PlexLog.warn("Failed to load notes for {0}: {1}", uuid, e.getMessage());
                return Lists.newArrayList();
            }
        }, executor);
    }

    public CompletableFuture<Void> deleteNote(int id, UUID uuid)
    {
        return CompletableFuture.runAsync(() ->
        {
            try
            {
                jdbi.useHandle(h -> h.createUpdate("DELETE FROM notes WHERE uuid = :u AND id = :id")
                        .bind("u", uuid.toString())
                        .bind("id", id)
                        .execute());
            }
            catch (JdbiException e)
            {
                PlexLog.warn("Failed to delete note {0} for {1}: {2}", id, uuid, e.getMessage());
            }
        }, executor);
    }

    public CompletableFuture<Void> addNote(Note note)
    {
        return CompletableFuture.runAsync(() ->
        {
            try
            {
                int nextId = jdbi.withHandle(h -> h.createQuery("SELECT COALESCE(MAX(id), 0) FROM notes WHERE uuid = :u")
                        .bind("u", note.getUuid().toString()).mapTo(Integer.class).one()) + 1;
                NoteEntity entity = toEntity(note);
                entity.setId(nextId);
                jdbi.useHandle(h -> h.createUpdate(
                                "INSERT INTO notes (id, uuid, written_by_uuid, note, timestamp) " +
                                        "VALUES (:id, :uuid, :writtenBy, :note, :ts)")
                        .bind("id", entity.getId())
                        .bind("uuid", entity.getUuid())
                        .bind("writtenBy", entity.getWrittenByUuid())
                        .bind("note", entity.getNote())
                        .bind("ts", entity.getTimestamp())
                        .execute());
                note.setId(nextId);
            }
            catch (JdbiException e)
            {
                PlexLog.warn("Failed to add note for {0}: {1}", note.getUuid(), e.getMessage());
            }
        }, executor);
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

    private Optional<Note> toNote(NoteEntity entity)
    {
        try
        {
            Note note = new Note(
                    UUID.fromString(entity.getUuid()),
                    entity.getNote(),
                    UUID.fromString(entity.getWrittenByUuid()),
                    ZonedDateTime.ofInstant(Instant.ofEpochMilli(entity.getTimestamp()), ZoneId.of(TimeUtils.TIMEZONE))
            );
            note.setId(entity.getId());
            return Optional.of(note);
        }
        catch (IllegalArgumentException | NullPointerException e)
        {
            return Optional.empty();
        }
    }

    private NoteEntity toEntity(Note note)
    {
        NoteEntity entity = new NoteEntity();
        entity.setId(note.getId());
        entity.setUuid(note.getUuid().toString());
        entity.setWrittenByUuid(note.getWrittenBy().toString());
        entity.setNote(note.getNote());
        entity.setTimestamp(note.getTimestamp().toInstant().toEpochMilli());
        return entity;
    }
}
