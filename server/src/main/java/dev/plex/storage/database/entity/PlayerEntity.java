package dev.plex.storage.database.entity;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlayerEntity
{
    private String uuid;
    private String lastKnownName;
    private String loginMessage;
    private String prefix;
    private boolean staffChat;
    private boolean commandSpy;

    public PlayerEntity()
    {
    }
}
