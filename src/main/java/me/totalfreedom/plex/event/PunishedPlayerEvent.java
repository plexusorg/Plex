package me.totalfreedom.plex.event;

import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import me.totalfreedom.plex.player.PunishedPlayer;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.player.PlayerEvent;

@Getter
public abstract class PunishedPlayerEvent extends PlayerEvent implements Cancellable
{
    protected PunishedPlayer punishedPlayer;
    @Setter
    protected boolean cancelled;

    protected PunishedPlayerEvent(PunishedPlayer punishedPlayer)
    {
        super(Bukkit.getPlayer(UUID.fromString(punishedPlayer.getUuid())));
        this.punishedPlayer = punishedPlayer;
    }
}