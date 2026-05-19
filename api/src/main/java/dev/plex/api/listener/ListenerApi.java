package dev.plex.api.listener;

import org.bukkit.event.Listener;

public interface ListenerApi
{
    void register(Listener listener);

    void unregister(Listener listener);
}
