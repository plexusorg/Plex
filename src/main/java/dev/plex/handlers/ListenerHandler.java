package dev.plex.handlers;

import com.google.common.collect.Lists;
import dev.plex.listener.impl.AdminListener;
import dev.plex.listener.impl.ChatListener;
import dev.plex.listener.impl.FreezeListener;
import dev.plex.listener.impl.LoginListener;
import dev.plex.listener.impl.PlayerListener;
import dev.plex.listener.impl.ServerListener;
import dev.plex.listener.impl.WorldListener;
import java.util.List;
import dev.plex.listener.PlexListener;
import dev.plex.util.PlexLog;

public class ListenerHandler
{
    List<PlexListener> listeners = Lists.newArrayList();
    public ListenerHandler()
    {
        listeners.add(new ServerListener());
        listeners.add(new ChatListener());
        listeners.add(new PlayerListener());
        listeners.add(new WorldListener());
        listeners.add(new FreezeListener());
        listeners.add(new AdminListener());
        listeners.add(new LoginListener());
        PlexLog.log(String.format("Registered %s listeners!", listeners.size()));
    }
}
