package dev.plex.listener;

import dev.plex.Plex;

public class ProxyListener
{
    protected final Plex plugin = Plex.get();

    public ProxyListener()
    {
        Plex.get().getServer().getEventManager().register(Plex.get(), this);
    }
}
