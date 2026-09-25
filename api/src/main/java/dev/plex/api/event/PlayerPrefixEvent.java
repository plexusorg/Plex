package dev.plex.api.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

/**
 * Supplies additive prefixes before Plex renders a player's chat line or tab-list entry.
 *
 * <p>{@link Target#TAB}: Plex calls this synchronous event on the player's owning region
 * at join and every 20 ticks.</p>
 *
 * <p>{@link Target#CHAT}: Plex calls this event for each rendered chat line. The event is
 * asynchronous when Plex renders a normal chat message. The event is synchronous when a
 * module calls {@code MessageApi.chatLine} on a thread that can read the player.</p>
 *
 * <p>For both targets, read cached state only. Do not perform database or network work.
 * Each event starts with no prefixes. Add active prefixes on each call; Plex does not
 * store them. Check {@link #getTarget()} to contribute to one target only. Unregister
 * listeners when their plugin or module stops.</p>
 *
 * <p>Plex renders prefixes in listener order, then the tag, then the name. Plex inserts
 * one space after each nonempty prefix and tag. In chat, the prefixes and the tag fill the
 * {@code <prefix>} placeholder of {@code chat.format}, and the format supplies the space
 * before the name. The base tag and name are read-only so independent plugins cannot
 * replace them through this event.</p>
 */
public final class PlayerPrefixEvent extends PlayerEvent
{
    private static final HandlerList HANDLERS = new HandlerList();

    private final Target target;
    private final Component name;
    private final Component tag;
    private final List<Component> prefixes = new ArrayList<>();

    /**
     * Creates a fresh prefix request.
     *
     * @param player player whose chat line or tab-list entry Plex will render
     * @param target output that Plex will render
     * @param name name that Plex renders after the tag
     * @param tag tag that Plex renders after the prefixes, or an empty component when no tag is set
     * @param async true when Plex calls the event off the server thread
     */
    public PlayerPrefixEvent(@NotNull Player player, @NotNull Target target, @NotNull Component name, @NotNull Component tag, boolean async)
    {
        super(Objects.requireNonNull(player, "player"), async);
        this.target = Objects.requireNonNull(target, "target");
        this.name = Objects.requireNonNull(name, "name");
        this.tag = Objects.requireNonNull(tag, "tag");
    }

    /**
     * Returns the output that Plex will render.
     * @return chat or tab target
     */
    public @NotNull Target getTarget()
    {
        return target;
    }

    /**
     * Returns the base name.
     * <p>For {@link Target#TAB}, this is the display name or the rank-colored username.
     * For {@link Target#CHAT}, this is the display name.</p>
     * @return base name
     */
    public @NotNull Component getName()
    {
        return name;
    }

    /**
     * Returns the base tag.
     * <p>For {@link Target#TAB}, this is the custom tag. For {@link Target#CHAT}, this is
     * the custom tag, or the rank prefix when no custom tag is set.</p>
     * @return tag without a separator, or an empty component
     */
    public @NotNull Component getTag()
    {
        return tag;
    }

    /**
     * Adds a prefix after prefixes from earlier listeners, before the tag.
     *
     * @param prefix prefix without a trailing separator; an empty component is ignored
     */
    public void addPrefix(@NotNull Component prefix)
    {
        Objects.requireNonNull(prefix, "prefix");
        if (!prefix.equals(Component.empty()))
        {
            prefixes.add(prefix);
        }
    }

    /**
     * Returns the prefixes contributed so far.
     * @return immutable snapshot of prefixes in rendering order
     */
    public @NotNull List<Component> getPrefixes()
    {
        return List.copyOf(prefixes);
    }

    @Override
    public @NotNull HandlerList getHandlers()
    {
        return HANDLERS;
    }

    /**
     * Returns the handler list for this event.
     * @return handler list for this event
     */
    public static @NotNull HandlerList getHandlerList()
    {
        return HANDLERS;
    }

    /**
     * Output that Plex renders with the prefixes.
     */
    public enum Target
    {
        /**
         * A public chat line.
         */
        CHAT,
        /**
         * The player's tab-list entry.
         */
        TAB
    }
}
