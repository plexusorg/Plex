package dev.plex.listener;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;

/**
 * Defines a Bukkit event handler that code registers directly.
 *
 * @param eventType event class handled by this rule
 * @param priority handler priority
 * @param ignoreCancelled whether canceled events are skipped
 * @param handler event handler
 * @param <E> event type
 */
public record EventRule<E extends Event>(Class<E> eventType, EventPriority priority, boolean ignoreCancelled, Consumer<E> handler)
{
    /**
     * Validates an event rule.
     */
    public EventRule
    {
        Objects.requireNonNull(eventType, "eventType");
        Objects.requireNonNull(priority, "priority");
        Objects.requireNonNull(handler, "handler");
    }

    /**
     * Creates an event rule.
     *
     * @param eventType event class handled by this rule
     * @param priority handler priority
     * @param ignoreCancelled whether canceled events are skipped
     * @param handler event handler
     * @param <E> event type
     * @return event rule
     */
    public static <E extends Event> EventRule<E> of(Class<E> eventType, EventPriority priority, boolean ignoreCancelled, Consumer<E> handler)
    {
        return new EventRule<>(eventType, priority, ignoreCancelled, handler);
    }

    /**
     * Creates an event rule that also receives canceled events.
     *
     * @param eventType event class handled by this rule
     * @param priority handler priority
     * @param handler event handler
     * @param <E> event type
     * @return event rule
     */
    public static <E extends Event> EventRule<E> of(Class<E> eventType, EventPriority priority, Consumer<E> handler)
    {
        return of(eventType, priority, false, handler);
    }

    /**
     * Creates a rule that cancels matching events.
     *
     * @param eventType event class handled by this rule
     * @param priority handler priority
     * @param ignoreCancelled whether canceled events are skipped
     * @param blocked returns whether the event must be canceled
     * @param <E> cancellable event type
     * @return blocking event rule
     */
    public static <E extends Event & Cancellable> EventRule<E> blocking(Class<E> eventType, EventPriority priority, boolean ignoreCancelled, Predicate<E> blocked)
    {
        Objects.requireNonNull(blocked, "blocked");
        return of(eventType, priority, ignoreCancelled, event ->
        {
            if (blocked.test(event))
            {
                event.setCancelled(true);
            }
        });
    }

    /**
     * Creates a blocking rule that also receives canceled events.
     *
     * @param eventType event class handled by this rule
     * @param priority handler priority
     * @param blocked returns whether the event must be canceled
     * @param <E> cancellable event type
     * @return blocking event rule
     */
    public static <E extends Event & Cancellable> EventRule<E> blocking(Class<E> eventType, EventPriority priority, Predicate<E> blocked)
    {
        return blocking(eventType, priority, false, blocked);
    }
}
