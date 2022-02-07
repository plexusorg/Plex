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
public class PunishedPlayerFreezeEvent extends PunishedPlayerEvent implements Cancellable
{
    private static final HandlerList handlers = new HandlerList();

    /**
     * New frozen state of the player
     */
    private final boolean frozen;

    /**
     * Creates a new event instance
     * @param punishedPlayer The player who was punished
     * @param frozen The new frozen status
     */
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