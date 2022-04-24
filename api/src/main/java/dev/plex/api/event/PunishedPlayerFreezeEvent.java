package dev.plex.api.event;

import dev.plex.api.player.IPlexPlayer;
import lombok.Getter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

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
     *
     * @param punishedPlayer The player who was punished
     * @param frozen         The new frozen status
     */
    public PunishedPlayerFreezeEvent(IPlexPlayer punishedPlayer, boolean frozen)
    {
        super(punishedPlayer);
        this.frozen = frozen;
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