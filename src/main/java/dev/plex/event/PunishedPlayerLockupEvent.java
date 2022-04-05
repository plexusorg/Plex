package dev.plex.event;

import dev.plex.player.PlexPlayer;
import lombok.Getter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

/**
 * Event that is called when a player is frozen or unfrozen
 */
@Getter
public class PunishedPlayerLockupEvent extends PunishedPlayerEvent implements Cancellable
{
    private static final HandlerList handlers = new HandlerList();

    /**
     * New lock up state of the player
     */
    private final boolean lockedUp;

    /**
     * Creates a new event instance
     *
     * @param punishedPlayer The player who was punished
     * @param lockedUp       The new muted status
     */
    public PunishedPlayerLockupEvent(PlexPlayer punishedPlayer, boolean lockedUp)
    {
        super(punishedPlayer);
        this.lockedUp = lockedUp;
    }

    public static HandlerList getHandlerList()
    {
        return handlers;
    }

    @Override
    public HandlerList getHandlers()
    {
        return handlers;
    }
}