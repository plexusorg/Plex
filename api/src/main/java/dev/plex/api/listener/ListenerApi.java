package dev.plex.api.listener;

import org.bukkit.event.Listener;

/**
 * Registers and unregisters Bukkit event listeners for modules.
 */
public interface ListenerApi
{
    /**
     * Registers a listener with Plex.
     *
     * @param listener listener to register
     */
    void register(Listener listener);

    /**
     * Unregisters a listener from Bukkit handler lists.
     *
     * @param listener listener to unregister
     */
    void unregister(Listener listener);
}
