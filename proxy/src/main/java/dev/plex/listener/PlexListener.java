package dev.plex.listener;

import dev.plex.Plex;

public class PlexListener
{
    protected final Plex plugin = Plex.get();

    public PlexListener()
    {
        Plex.get().getServer().getEventManager().register(Plex.get(), this);
    }
}
