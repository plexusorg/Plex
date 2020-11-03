package me.totalfreedom.plex.event;

import lombok.Getter;
import me.totalfreedom.plex.player.PunishedPlayer;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
@Getter
public class PunishedPlayerFreezeEvent extends PunishedPlayerEvent implements Cancellable
{
    private static final HandlerList handlers = new HandlerList();

    /**
     * Status of the Punished Player being frozen before the event's occurrence.
     */
    private final boolean frozen;

    public PunishedPlayerFreezeEvent(PunishedPlayer punishedPlayer, boolean frozen)
    {
        super(punishedPlayer);
        this.frozen = frozen;
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