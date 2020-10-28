package me.totalfreedom.plex.player;

import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PunishedPlayer
{
    //everything in here will be stored in redis
    @Setter(AccessLevel.NONE)
    private String uuid;

    private boolean muted;
    private boolean frozen;

    public PunishedPlayer(UUID playerUUID)
    {
        this.uuid = playerUUID.toString();
        this.muted = false;
        this.frozen = false;
    }

}
