package dev.plex.handlers;

import dev.plex.Plex;
import dev.plex.listener.ProxyListener;
import dev.plex.listener.impl.ConnectionListener;
import dev.plex.listener.impl.ServerListener;
import dev.plex.util.PlexLog;
import java.util.ArrayList;
import java.util.List;

public class ListenerHandler
{
    private final Plex plugin;
    private final List<ProxyListener> listeners = new ArrayList<>();

    public ListenerHandler(Plex plugin)
    {
        this.plugin = plugin;
        registerBuiltInListeners();
        PlexLog.log("Registered " + listeners.size() + " proxy listeners.");
    }

    private void registerBuiltInListeners()
    {
        listeners.add(new ConnectionListener(plugin));
        listeners.add(new ServerListener(plugin));
    }
}
