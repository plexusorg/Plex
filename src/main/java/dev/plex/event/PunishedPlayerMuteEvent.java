package dev.plex.event;

import dev.plex.player.PunishedPlayer;
import lombok.Getter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

//TODO: Event doesn't look like it is called

/**
 * Event that is called when a player is frozen or unfrozen
 */
@Getter
public class PunishedPlayerMuteEvent extends PunishedPlayerEvent implements Cancellable
{
    private static final HandlerList handlers = new HandlerList();

    /**
     * New muted state of the player
     */
    private final boolean muted;

    /**
     * Creates a new event instance
     *
     * @param punishedPlayer The player who was punished
     * @param muted          The new muted status
     */
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