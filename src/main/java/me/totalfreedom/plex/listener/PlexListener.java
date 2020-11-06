package me.totalfreedom.plex.listener;

import me.totalfreedom.plex.PlexBase;
import org.bukkit.event.Listener;

public abstract class PlexListener extends PlexBase implements Listener
{
    public PlexListener()
    {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
}
