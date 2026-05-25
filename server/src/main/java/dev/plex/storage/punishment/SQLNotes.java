package dev.plex.storage.punishment;

import com.google.common.collect.Lists;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.stmt.DeleteBuilder;
import dev.plex.punishment.extra.Note;
import dev.plex.storage.database.entity.NoteEntity;
import dev.plex.storage.repository.NoteRepository;
import dev.plex.util.TimeUtils;

import java.sql.SQLException;
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
    private final Dao<NoteEntity, Long> notes;
    private final Executor executor;

    public SQLNotes(ConnectionSource connectionSource, Executor executor)
    {
        try
        {
            this.notes = DaoManager.createDao(connectionSource, NoteEntity.class);
            this.executor = executor;
        }
        catch (SQLException e)
        {
            throw new IllegalStateException("Failed to create note DAO", e);
        }
    }

    public CompletableFuture<List<Note>> getNotes(UUID uuid)
    {
        return CompletableFuture.supplyAsync(() ->
        {
            try
            {
                return notes.queryForEq("uuid", uuid.toString()).stream()
                        .sorted(Comparator.comparingInt(NoteEntity::getId))
                        .map(this::toNote)
                        .flatMap(Optional::stream)
                        .toList();
            }
            catch (SQLException e)
            {
                e.printStackTrace();
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
                DeleteBuilder<NoteEntity, Long> delete = notes.deleteBuilder();
                delete.where().eq("uuid", uuid.toString()).and().eq("id", id);
                delete.delete();
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
        }, executor);
    }

    public CompletableFuture<Void> addNote(Note note)
    {
        return CompletableFuture.runAsync(() ->
        {
            try
            {
                int nextId = notes.queryForEq("uuid", note.getUuid().toString()).stream()
                        .map(NoteEntity::getId)
                        .max(Integer::compareTo)
                        .orElse(0) + 1;
                NoteEntity entity = toEntity(note);
                entity.setId(nextId);
                notes.create(entity);
                note.setId(nextId);
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
        }, executor);
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
