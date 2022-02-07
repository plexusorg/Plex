package dev.plex.handlers;

import com.google.common.collect.Lists;
import dev.plex.listener.PlexListener;
import dev.plex.listener.impl.AdminListener;
import dev.plex.listener.impl.BanListener;
import dev.plex.listener.impl.ChatListener;
import dev.plex.listener.impl.CommandListener;
import dev.plex.listener.impl.FreezeListener;
import dev.plex.listener.impl.PlayerListener;
import dev.plex.listener.impl.ServerListener;
import dev.plex.listener.impl.WorldListener;
import dev.plex.util.PlexLog;
import java.util.List;

public class ListenerHandler
{
    public ListenerHandler()
    {
        List<PlexListener> listeners = Lists.newArrayList();
        listeners.add(new ServerListener());
        listeners.add(new ChatListener());
        listeners.add(new CommandListener());
        listeners.add(new PlayerListener());
        listeners.add(new WorldListener());
        listeners.add(new FreezeListener());
        listeners.add(new AdminListener());
        listeners.add(new BanListener());
        PlexLog.log(String.format("Registered %s listeners!", listeners.size()));
    }
}
