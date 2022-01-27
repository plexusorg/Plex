package dev.plex.event;

import dev.plex.player.PlexPlayer;
import lombok.Data;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Data
public class AdminRemoveEvent extends Event
{
    private static final HandlerList handlers = new HandlerList();

    private final CommandSender sender;
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
