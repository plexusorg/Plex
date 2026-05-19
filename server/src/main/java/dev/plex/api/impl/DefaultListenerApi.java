package dev.plex.api.impl;

import dev.plex.Plex;
import dev.plex.api.listener.ListenerApi;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

final class DefaultListenerApi implements ListenerApi
{
    private final Plex plugin;

    DefaultListenerApi(Plex plugin) { this.plugin = plugin; }

    @Override
    public void register(Listener listener) { plugin.getServer().getPluginManager().registerEvents(listener, plugin); }

    @Override
    public void unregister(Listener listener) { HandlerList.unregisterAll(listener); }
}
