package dev.plex.storage.database.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerIpEntity
{
    private long id;
    private String playerUuid;
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
