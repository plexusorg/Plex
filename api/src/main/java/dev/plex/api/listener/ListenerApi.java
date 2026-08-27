package dev.plex.api.listener;

import org.bukkit.event.Listener;

/**
 * Registers and unregisters Bukkit event listeners for modules.
 * Modules must use the methods on {@link dev.plex.module.PlexModule} so that
 * Plex can unregister their listeners during module unload.
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
     * Registers event rules owned by a listener.
     *
     * @param listener listener that owns the registrations
     * @param rules event rules to register
     */
    void register(Listener listener, EventRule<?>... rules);

    /**
     * Registers event rules with a new listener owner.
     *
     * @param rules event rules to register
     * @return listener that owns the registrations
     */
    Listener register(EventRule<?>... rules);

    /**
     * Unregisters a listener from Bukkit handler lists.
     *
     * @param listener listener to unregister
     */
    void unregister(Listener listener);
}
