package dev.plex.storage.database.entity;

import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@DatabaseTable(tableName = "players")
public class PlayerEntity
{
    @DatabaseField(id = true, columnName = "uuid", width = 46)
    private String uuid;

    @DatabaseField(columnName = "name", width = 18)
    private String name;

    @DatabaseField(columnName = "login_msg", width = 2000)
    private String loginMessage;

    @DatabaseField(columnName = "prefix", width = 2000)
    private String prefix;

    @DatabaseField(columnName = "staffChat")
    private boolean staffChat;

    @DatabaseField(columnName = "ips", width = 2000)
    private String ips;

    @DatabaseField(columnName = "coins")
    private long coins;

    @DatabaseField(columnName = "vanished")
    private boolean vanished;

    @DatabaseField(columnName = "commandspy")
    private boolean commandSpy;

    public PlayerEntity()
    {
    }
}
