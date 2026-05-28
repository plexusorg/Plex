package dev.plex.storage.database.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NoteEntity
{
    private long rowId;
    private int id;
    private String uuid;
    private String writtenByUuid;
    private String note;
    private long timestamp;

    public NoteEntity()
    {
    }
}
