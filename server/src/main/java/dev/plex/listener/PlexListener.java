package dev.plex.listener;

import dev.plex.Plex;
import org.bukkit.event.Listener;

public abstract class PlexListener implements Listener
{
    protected final Plex plugin;

    public PlexListener()
    {
        this(Plex.get());
    }

    protected PlexListener(Plex plugin)
    {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
}
