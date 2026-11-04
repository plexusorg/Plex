package dev.plex.api.event;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * Fires when a player sends a message on a module's chat channel, such as a guild chat.
 *
 * <p>A channel is a chat that only some players see instead of everyone on the server. The module that owns the channel
 * fires this event. Listeners use it to inspect or filter every channel with one handler.</p>
 *
 * <h2>Contract for the firing module</h2>
 * <ul>
 *   <li>Fire the event once for each message, before any delivery: before the broadcast to the conversation, any spy or
 *   staff copy, and any console log line of the message.</li>
 *   <li>Pass the player who sent the message, a {@link Key} in the module's own namespace as the channel, and an
 *   identifier of the conversation. All players who see a message share its conversation.</li>
 *   <li>Pass {@code true} for {@code async} when the event is fired off the server and region threads, and
 *   {@code false} when it is fired on one of them. Bukkit rejects a synchronous event fired from another thread.</li>
 *   <li>When a listener cancels the event, send the message to the sender alone, in the channel's normal format, and
 *   nowhere else. The sender must see no difference from a delivered message.</li>
 * </ul>
 *
 * <h2>Contract for listeners</h2>
 * <ul>
 *   <li>The event is fired off the server and region threads when {@link #isAsynchronous()} is {@code true}. Check it
 *   before you use APIs that need a server or region thread.</li>
 *   <li>Cancel the event to keep the message from the rest of the conversation. Do not send the message back to the
 *   sender from the listener, because the firing module already does.</li>
 *   <li>The event reports the message as the player wrote it. It cannot change the message.</li>
 * </ul>
 */
public final class ChatChannelMessageEvent extends Event implements Cancellable
{
    private static final HandlerList HANDLERS = new HandlerList();

    private final Player player;
    private final Key channel;
    private final String conversation;
    private final Component message;
    private boolean cancelled;

    /**
     * Creates a chat channel message event.
     *
     * @param player the player who sent the message
     * @param channel the channel, in the namespace of the module that owns it, such as {@code guilds:chat}
     * @param conversation the identifier of the conversation inside the channel, such as a guild UUID
     * @param message the message as the player wrote it
     * @param async whether the event is fired off the server and region threads
     */
    public ChatChannelMessageEvent(
            @NotNull Player player,
            @NotNull Key channel,
            @NotNull String conversation,
            @NotNull Component message,
            boolean async)
    {
        super(async);
        this.player = Objects.requireNonNull(player, "player");
        this.channel = Objects.requireNonNull(channel, "channel");
        this.conversation = Objects.requireNonNull(conversation, "conversation");
        this.message = Objects.requireNonNull(message, "message");
    }

    /**
     * Returns the player who sent the message.
     *
     * @return message sender
     */
    public @NotNull Player getPlayer()
    {
        return player;
    }

    /**
     * Returns the channel the message was sent on.
     *
     * @return channel key, in the namespace of the module that owns the channel
     */
    public @NotNull Key getChannel()
    {
        return channel;
    }

    /**
     * Returns the conversation that receives the message. The identifier is unique inside its channel, and the
     * players who see the message are the players of this conversation.
     *
     * @return conversation identifier
     */
    public @NotNull String getConversation()
    {
        return conversation;
    }

    /**
     * Returns the message as the player wrote it.
     *
     * @return message
     */
    public @NotNull Component getMessage()
    {
        return message;
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
}
