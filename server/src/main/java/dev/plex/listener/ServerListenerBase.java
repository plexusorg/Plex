package dev.plex.listener;

import dev.plex.Plex;
import org.bukkit.event.Listener;

public abstract class ServerListenerBase implements Listener
{
    protected final Plex plugin;

    public ServerListenerBase()
    {
        this(Plex.get());
    }

    protected ServerListenerBase(Plex plugin)
    {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
}
