package dev.plex.punishment.extra;

import com.google.gson.GsonBuilder;
import dev.morphia.annotations.Entity;
import java.time.ZonedDateTime;
import java.util.UUID;

import dev.plex.util.adapter.ZonedDateTimeSerializer;
import lombok.Data;

@Data
@Entity
public class Note
{
    private final UUID uuid;
    private final String note;
    private final UUID writtenBy;
    private final ZonedDateTime timestamp;

    private int id; // This will be automatically set from addNote

    public String toJSON()
    {
        return new GsonBuilder().registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeSerializer()).create().toJson(this);
    }
}
