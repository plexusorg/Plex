package dev.plex.api.event;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired before Plex delivers a locally-originated staff-chat message.
 *
 * <p>The event covers both a player's toggled staff-chat input and the direct
 * {@code /adminchat <message>} command form. It is not fired when a staff-chat
 * message is received from another server through Plex's Redis transport.</p>
 *
 * <p>This event can be asynchronous. Listeners must check
 * {@link #isAsynchronous()} before accessing APIs which require a server or
 * region thread.</p>
 */
public final class StaffChatMessageEvent extends Event implements Cancellable
{
    private static final HandlerList HANDLERS = new HandlerList();

    private final CommandSender sender;
    private final Source source;
    private Component message;
    private boolean cancelled;

    /**
     * Creates a staff-chat message event.
     *
     * @param sender sender of the message
     * @param message message to be delivered
     * @param source input path which produced the message
     * @param async whether the event is asynchronous
     */
    public StaffChatMessageEvent(
            @NotNull CommandSender sender,
            @NotNull Component message,
            @NotNull Source source,
            boolean async)
    {
        super(async);
        this.sender = sender;
        this.message = message;
        this.source = source;
    }

    /**
     * Returns the player or console which sent the message.
     *
     * @return message sender
     */
    public @NotNull CommandSender getSender()
    {
        return sender;
    }

    /**
     * Returns the message which Plex will deliver.
     *
     * @return staff-chat message
     */
    public @NotNull Component getMessage()
    {
        return message;
    }

    /**
     * Replaces the message Plex will deliver.
     *
     * @param message replacement message
     */
    public void setMessage(@NotNull Component message)
    {
        this.message = message;
    }

    /**
     * Returns how the message entered staff chat.
     *
     * @return message source
     */
    public @NotNull Source getSource()
    {
        return source;
    }

    @Override
    public boolean isCancelled()
    {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled)
    {
        this.cancelled = cancelled;
    }

    @Override
    public @NotNull HandlerList getHandlers()
    {
        return HANDLERS;
    }

    /**
     * Returns this event's handler list.
     *
     * @return handler list
     */
    public static @NotNull HandlerList getHandlerList()
    {
        return HANDLERS;
    }

    /**
     * Identifies the Plex input path which produced a staff-chat message.
     */
    public enum Source
    {
        /** A player spoke while staff-chat mode was enabled. */
        TOGGLED_CHAT,

        /** A sender used {@code /adminchat <message>} or one of its aliases. */
        COMMAND
    }
}
