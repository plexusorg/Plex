package dev.plex.cache.sql;

import com.google.common.collect.Lists;
import dev.plex.Plex;
import dev.plex.punishment.extra.Note;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class SQLNotes
{
    private static final String SELECT = "SELECT * FROM `notes` WHERE uuid=?";

    private static final String INSERT = "INSERT INTO `notes` (`uuid`, `written_by`, `note`, `timestamp`) VALUES(?, ?, ?, ?)";
    private static final String DELETE = "DELETE FROM `notes` WHERE uuid=? AND note=?";

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
                            LocalDateTime.ofInstant(Instant.ofEpochMilli(set.getLong("timestamp")), ZoneId.systemDefault())
                    );
                    notes.add(note);
                }
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
            return notes;
        });
    }

    public CompletableFuture<Void> addNote(Note note)
    {
        return CompletableFuture.runAsync(() ->
        {
            try (Connection con = Plex.get().getSqlConnection().getCon())
            {
                PreparedStatement statement = con.prepareStatement(INSERT);
                statement.setString(1, note.getUuid().toString());
                statement.setString(2, note.getWrittenBy().toString());
                statement.setString(3, note.getNote());
                statement.setLong(4, note.getTimestamp().toInstant(ZoneOffset.UTC).toEpochMilli());
                statement.execute();
            }
            catch (SQLException e)
            {
                e.printStackTrace();
            }
        });
    }
}
