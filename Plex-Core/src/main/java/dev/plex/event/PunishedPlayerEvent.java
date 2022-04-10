package dev.plex.event;

import dev.plex.player.PlexPlayer;
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
    protected PlexPlayer punishedPlayer;

    /**
     * Whether the event was cancelled
     */
    @Setter
    protected boolean cancelled; //TODO: unsure if cancelling the event does anything

    /**
     * Creates an event object
     *
     * @param punishedPlayer The player who was punished
     * @see PlexPlayer
     */
    protected PunishedPlayerEvent(PlexPlayer punishedPlayer)
    {
        super(Bukkit.getPlayer(punishedPlayer.getUuid()));
        this.punishedPlayer = punishedPlayer;
    }
}