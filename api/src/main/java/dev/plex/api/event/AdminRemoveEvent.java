package dev.plex.api.event;

import dev.plex.api.player.IPlexPlayer;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event that is run when a player is removed from the admin list
 */
@EqualsAndHashCode(callSuper = false)
@Data
public class AdminRemoveEvent extends Event
{
    private static final HandlerList handlers = new HandlerList();

    /**
     * The sender who added the player
     */
    private final CommandSender sender;

    /**
     * The PlexPlayer that was removed
     */
    private final IPlexPlayer plexPlayer;

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
