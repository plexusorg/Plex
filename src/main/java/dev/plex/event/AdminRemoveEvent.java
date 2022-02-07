package dev.plex.event;

import dev.plex.player.PlexPlayer;
import lombok.Data;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event that is ran when a player is removed from the admin list
 */
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
    private final PlexPlayer plexPlayer;

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
