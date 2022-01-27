package dev.plex.event;

import dev.plex.player.PlexPlayer;
import dev.plex.rank.enums.Rank;
import lombok.Data;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

@Data
public class AdminSetRankEvent extends Event
{
    private static final HandlerList handlers = new HandlerList();

    private final CommandSender sender;
    private final PlexPlayer plexPlayer;
    private final Rank rank;


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
