package me.totalfreedom.plex.event;

import me.totalfreedom.plex.player.PlexPlayer;
import me.totalfreedom.plex.rank.enums.Rank;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class AdminSetRankEvent extends Event
{
    private final HandlerList handlers = new HandlerList();

    private PlexPlayer plexPlayer;
    private Rank rank;

    public AdminSetRankEvent(PlexPlayer plexPlayer, Rank rank)
    {
        this.plexPlayer = plexPlayer;
        this.rank = rank;
    }

    @Override
    public HandlerList getHandlers()
    {
        return handlers;
    }

    public PlexPlayer getPlexPlayer() {
        return plexPlayer;
    }

    public Rank getRank() {
        return rank;
    }
}
