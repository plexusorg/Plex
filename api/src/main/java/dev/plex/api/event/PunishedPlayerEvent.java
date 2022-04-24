package dev.plex.api.event;

import dev.plex.api.player.IPlexPlayer;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.player.PlayerEvent;

/**
 * Superclass for punishment events
 */
@Getter
public abstract class PunishedPlayerEvent extends PlayerEvent implements Cancellable
{
    /**
     * The player who was punished
     */
    protected IPlexPlayer punishedPlayer;

    /**
     * Whether the event was cancelled
     */
    @Setter
    protected boolean cancelled; //TODO: unsure if cancelling the event does anything

    /**
     * Creates an event object
     *
     * @param punishedPlayer The player who was punished
     * @see IPlexPlayer
     */
    protected PunishedPlayerEvent(IPlexPlayer punishedPlayer)
    {
        super(Bukkit.getPlayer(punishedPlayer.getUuid()));
        this.punishedPlayer = punishedPlayer;
    }
}