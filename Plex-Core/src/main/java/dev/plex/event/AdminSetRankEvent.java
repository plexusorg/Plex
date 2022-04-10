package dev.plex.event;

import dev.plex.player.PlexPlayer;
import dev.plex.rank.enums.Rank;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event that is run when an admins rank is set
 */
@EqualsAndHashCode(callSuper = false)
@Data
public class AdminSetRankEvent extends Event
{
    private static final HandlerList handlers = new HandlerList();

    /**
     * The sender who added the player
     */
    private final CommandSender sender;

    /**
     * The PlexPlayer that was removed
     */
    private final PlexPlayer plexPlayer;

    /**
     * The rank the player was set to
     */
    private final Rank rank;

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
