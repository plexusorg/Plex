package me.totalfreedom.plex.event;

import me.totalfreedom.plex.command.source.CommandSource;
import me.totalfreedom.plex.player.PlexPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class AdminAddEvent extends Event
{
    private static final HandlerList handlers = new HandlerList();

    private CommandSource sender;
    private PlexPlayer plexPlayer;

    public AdminAddEvent(CommandSource sender, PlexPlayer plexPlayer)
    {
        this.sender = sender;
        this.plexPlayer = plexPlayer;
    }

    @Override
    public HandlerList getHandlers()
    {
        return handlers;
    }

    public static HandlerList getHandlerList()
    {
        return handlers;
    }

    public PlexPlayer getPlexPlayer()
    {
        return plexPlayer;
    }

    public CommandSource getSender() {
        return sender;
    }
}
