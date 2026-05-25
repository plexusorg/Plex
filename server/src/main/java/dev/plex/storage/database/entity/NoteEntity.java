package dev.plex.storage.database.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@DatabaseTable(tableName = "notes")
public class NoteEntity
{
    @DatabaseField(generatedId = true, columnName = "row_id")
    private long rowId;

    @DatabaseField(columnName = "id", index = true)
    private int id;

    @DatabaseField(columnName = "uuid", canBeNull = false, index = true, width = 46)
    private String uuid;

    @DatabaseField(columnName = "written_by_uuid", width = 46)
    private String writtenByUuid;

    @DatabaseField(columnName = "note", width = 2000)
    private String note;

    @DatabaseField(columnName = "timestamp")
    private long timestamp;

    public NoteEntity()
    {
    }
}
