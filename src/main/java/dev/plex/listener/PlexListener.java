package dev.plex.listener;

import dev.plex.PlexBase;
import org.bukkit.event.Listener;

public abstract class PlexListener implements Listener, PlexBase
{
    public PlexListener()
    {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
}
