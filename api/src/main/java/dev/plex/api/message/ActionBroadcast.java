package dev.plex.api.message;

import net.kyori.adventure.text.Component;

/** An administrative announcement whose recipient policy was captured when the action started. */
@FunctionalInterface
public interface ActionBroadcast
{
    /**
     * Sends an announcement using the captured policy. Safe to call from an asynchronous completion;
     * callers remain responsible for waiting for the action to succeed before announcing it.
     *
     * @param message announcement to send
     */
    void send(Component message);
}
