package me.totalfreedom.plex.player;

import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import me.totalfreedom.plex.event.PunishedPlayerFreezeEvent;
import me.totalfreedom.plex.event.PunishedPlayerMuteEvent;
import org.bukkit.Bukkit;

@Getter
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

    public void setFrozen(boolean frozen)
    {
        PunishedPlayerFreezeEvent e = new PunishedPlayerFreezeEvent(this, this.frozen);
        Bukkit.getServer().getPluginManager().callEvent(e);
        if (!e.isCancelled())
        {
            this.frozen = frozen;
        }
    }

    public void setMuted(boolean muted)
    {
        PunishedPlayerMuteEvent e = new PunishedPlayerMuteEvent(this, this.muted);
        Bukkit.getServer().getPluginManager().callEvent(e);
        if (!e.isCancelled())
        {
            this.muted = muted;
        }
    }
}
