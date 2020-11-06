package me.totalfreedom.plex.handlers;

import com.google.common.collect.Lists;
import java.util.List;
import me.totalfreedom.plex.listener.PlexListener;
import me.totalfreedom.plex.listener.impl.ChatListener;
import me.totalfreedom.plex.listener.impl.FreezeListener;
import me.totalfreedom.plex.listener.impl.PlayerListener;
import me.totalfreedom.plex.listener.impl.ServerListener;
import me.totalfreedom.plex.listener.impl.WorldListener;
import me.totalfreedom.plex.util.PlexLog;

public class ListenerHandler
{
    private List<PlexListener> listeners = Lists.newArrayList();

    public ListenerHandler()
    {
        listeners.add(new ServerListener());
        listeners.add(new ChatListener());
        listeners.add(new PlayerListener());
        listeners.add(new WorldListener());
        listeners.add(new FreezeListener());
        PlexLog.log(String.format("Registered %s listeners!", listeners.size()));
    }
}
