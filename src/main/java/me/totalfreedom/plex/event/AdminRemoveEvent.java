package me.totalfreedom.plex.event;

import me.totalfreedom.plex.player.PlexPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class AdminRemoveEvent extends Event
{
    private final HandlerList handlers = new HandlerList();

    private PlexPlayer plexPlayer;

    public AdminRemoveEvent(PlexPlayer plexPlayer)
    {
        this.plexPlayer = plexPlayer;
    }

    @Override
    public HandlerList getHandlers()
    {
        return handlers;
    }

    public PlexPlayer getPlexPlayer() {
        return plexPlayer;
    }
}
