package dev.plex.event;

import dev.plex.player.PunishedPlayer;
import lombok.Getter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

@Getter
public class PunishedPlayerMuteEvent extends PunishedPlayerEvent implements Cancellable
{
    private static final HandlerList handlers = new HandlerList();

    /**
     * Status of the Punished Player being frozen before the event's occurrence.
     */
    private final boolean muted;

    public PunishedPlayerMuteEvent(PunishedPlayer punishedPlayer, boolean muted)
    {
        super(punishedPlayer);
        this.muted = muted;
    }

    @Override
    public HandlerList getHandlers()
    {
        return handlers;
    }

    public static HandlerList getHandlerList()
    {
        return handlers;
    }
}