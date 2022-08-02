package dev.plex.event;

import dev.plex.player.PlexPlayer;
import lombok.Getter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

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
    public PunishedPlayerMuteEvent(PlexPlayer punishedPlayer, boolean muted)
    {
        super(punishedPlayer);
        this.muted = muted;
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