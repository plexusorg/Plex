package dev.plex.storage.database.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@DatabaseTable(tableName = "player_ips")
public class PlayerIpEntity
{
    @DatabaseField(generatedId = true, columnName = "id")
    private long id;

    @DatabaseField(columnName = "player_uuid", canBeNull = false, index = true, width = 46)
    private String playerUuid;

    @DatabaseField(columnName = "ip", canBeNull = false, index = true, width = 64)
    private String ip;

    public PlayerIpEntity()
    {
    }

    public PlayerIpEntity(String playerUuid, String ip)
    {
        this.playerUuid = playerUuid;
        this.ip = ip;
    }
}
