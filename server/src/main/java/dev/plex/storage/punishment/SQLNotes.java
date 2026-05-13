package dev.plex.storage.punishment;

import com.google.common.collect.Lists;
import dev.plex.Plex;
import dev.plex.punishment.extra.Note;
import dev.plex.util.TimeUtils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SQLNotes
{
    private static final String SELECT = "SELECT * FROM `notes` WHERE uuid=?";
    private static final String INSERT = "INSERT INTO `notes` (`id`, `uuid`, `written_by`, `note`, `timestamp`) VALUES(?, ?, ?, ?, ?)";
    private static final String DELETE = "DELETE FROM `notes` WHERE uuid=? AND id=?";

    public CompletableFuture<List<Note>> getNotes(UUID uuid)
    {
        return CompletableFuture.supplyAsync(() ->
        {
            List<Note> notes = Lists.newArrayList();
            try (Connection con = Plex.get().getSqlConnection().getCon())
            {
                PreparedStatement statement = con.prepareStatement(SELECT);
                statement.setString(1, uuid.toString());
                ResultSet set = statement.executeQuery();
                while (set.next())
                {
                    Note note = new Note(
                            uuid,
                            set.getString("note"),
                            UUID.fromString(set.getString("written_by")),
                            ZonedDateTime.ofInstant(Instant.ofEpochMilli(set.getLong("timestamp")), ZoneId.of(TimeUtils.TIMEZONE))
                    );
                    note.setId(set.getInt("id"));
                    notes.add(note);
                }
            }
            catch (SQLException e)
            {
                e.printStackTrace();
                return notes;
            }
            return notes;
        });
    }

    public CompletableFuture<Void> deleteNote(int id, UUID uuid)
    {
        return CompletableFuture.runAsync(() ->
        {
            try (Connection con = Plex.get().getSqlConnection().getCon())
            {
                PreparedStatement statement = con.prepareStatement(DELETE);
                statement.setString(1, uuid.toString());
                statement.setInt(2, id);
                statement.execute();
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
        });
    }

    public CompletableFuture<Void> addNote(Note note)
    {
        return CompletableFuture.runAsync(() ->
        {
            getNotes(note.getUuid()).whenComplete((notes, throwable) ->
            {
                try (Connection con = Plex.get().getSqlConnection().getCon())
                {
                    PreparedStatement statement = con.prepareStatement(INSERT);
                    statement.setInt(1, notes.size() + 1);
                    statement.setString(2, note.getUuid().toString());
                    statement.setString(3, note.getWrittenBy().toString());
                    statement.setString(4, note.getNote());
                    statement.setLong(5, note.getTimestamp().toInstant().toEpochMilli());
                    statement.execute();
                    note.setId(notes.size());
                }
                catch (SQLException e)
                {
                    e.printStackTrace();
                }
            });
        });
    }
}
