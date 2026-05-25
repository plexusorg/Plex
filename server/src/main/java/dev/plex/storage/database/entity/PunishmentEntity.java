package dev.plex.storage.database.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@DatabaseTable(tableName = "punishments")
public class PunishmentEntity
{
    @DatabaseField(generatedId = true, columnName = "id")
    private long id;

    @DatabaseField(columnName = "punished_uuid", canBeNull = false, index = true, width = 46)
    private String punishedUuid;

    @DatabaseField(columnName = "punisher_uuid", width = 46)
    private String punisherUuid;

    @DatabaseField(columnName = "source", width = 20)
    private String source;

    @DatabaseField(columnName = "punisher_reference", width = 200)
    private String punisherReference;

    @DatabaseField(columnName = "ip", width = 2000, index = true)
    private String ip;

    @DatabaseField(columnName = "type", width = 30)
    private String type;

    @DatabaseField(columnName = "reason", width = 2000)
    private String reason;

    @DatabaseField(columnName = "customTime")
    private boolean customTime;

    @DatabaseField(columnName = "active", index = true)
    private boolean active;

    @DatabaseField(columnName = "issueDate")
    private long issueDate;

    @DatabaseField(columnName = "endDate")
    private long endDate;

    public PunishmentEntity()
    {
    }
}
