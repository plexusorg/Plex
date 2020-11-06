package me.totalfreedom.plex.event;

import me.totalfreedom.plex.command.source.CommandSource;
import me.totalfreedom.plex.player.PlexPlayer;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class AdminRemoveEvent extends Event
{
    private static final HandlerList handlers = new HandlerList();

    private PlexPlayer plexPlayer;
    private CommandSource sender;

    public AdminRemoveEvent(CommandSource sender, PlexPlayer plexPlayer)
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
