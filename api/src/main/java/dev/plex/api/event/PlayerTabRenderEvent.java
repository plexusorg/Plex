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
 * Supplies additive prefixes before Plex renders a player's tab-list entry.
 *
 * <p>Plex calls this synchronous event on the player's owning region at join and
 * every 20 ticks. Read cached state only. Do not perform database or network work.
 * Each event starts with no prefixes. Add active prefixes on each call; Plex does
 * not store them. Unregister listeners when their plugin or module stops.</p>
 *
 * <p>Plex renders prefixes in listener order, then the custom tag, then the display
 * name. Plex inserts one space after each nonempty prefix and tag. The base tag and
 * name are read-only so independent plugins cannot replace them through this event.
 * Plex sends an update only when the rendered entry changes.</p>
 */
public final class PlayerTabRenderEvent extends PlayerEvent
{
    private static final HandlerList HANDLERS = new HandlerList();

    private final Component name;
    private final Component tag;
    private final List<Component> prefixes = new ArrayList<>();

    /**
     * Creates a fresh tab rendering request.
     *
     * @param player player whose entry Plex will render
     * @param name player display name, with rank color applied only to the plain username
     * @param tag custom tag, or an empty component when no tag is set
     */
    public PlayerTabRenderEvent(@NotNull Player player, @NotNull Component name, @NotNull Component tag)
    {
        super(Objects.requireNonNull(player, "player"));
        this.name = Objects.requireNonNull(name, "name");
        this.tag = Objects.requireNonNull(tag, "tag");
    }

    /**
     * Returns the base display name.
     * @return base display name or rank-colored username
     */
    public @NotNull Component getName()
    {
        return name;
    }

    /**
     * Returns the base custom tag.
     * @return custom tag without a separator, or an empty component
     */
    public @NotNull Component getTag()
    {
        return tag;
    }

    /**
     * Adds a prefix after prefixes from earlier listeners, before the custom tag.
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
}
