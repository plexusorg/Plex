package dev.plex.listener;

import dev.plex.Plex;

public class ProxyListener
{
    protected final Plex plugin;

    protected ProxyListener(Plex plugin)
    {
        this.plugin = plugin;
        plugin.getServer().getEventManager().register(plugin, this);
    }
}
