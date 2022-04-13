package dev.plex.punishment.extra;

import com.google.gson.GsonBuilder;
import dev.morphia.annotations.Entity;
import dev.plex.util.adapter.LocalDateTimeSerializer;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Data;

@Data
@Entity
public class Note
{
    private final UUID uuid;
    private final String note;
    private final UUID writtenBy;
    private final LocalDateTime timestamp;

    private int id; // This will be automatically set from addNote

    public String toJSON()
    {
        return new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer()).create().toJson(this);
    }
}
