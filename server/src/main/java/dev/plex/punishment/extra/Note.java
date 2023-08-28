package dev.plex.punishment.extra;

import com.google.gson.GsonBuilder;
import dev.plex.storage.annotation.NoLimit;
import dev.plex.storage.annotation.SQLTable;
import dev.plex.util.adapter.ZonedDateTimeAdapter;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
@SQLTable("notes")
public class Note
{
    private final UUID uuid;

    @NoLimit
    private final String note;
    private final UUID writtenBy;
    private final ZonedDateTime timestamp;

    private int id; // This will be automatically set from addNote

    public String toJSON()
    {
        return new GsonBuilder().registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeAdapter()).create().toJson(this);
    }
}
