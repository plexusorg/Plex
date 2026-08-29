package dev.plex.api.impl;

import dev.plex.Plex;
import dev.plex.api.listener.EventRule;
import dev.plex.api.listener.ListenerApi;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class DefaultListenerApi implements ListenerApi
{
    private final Plex plugin;
    private final Set<Listener> registeredListeners = Collections.newSetFromMap(new IdentityHashMap<>());
    private final Map<Listener, Set<EventRule<?>>> registeredRules = new IdentityHashMap<>();

    DefaultListenerApi(Plex plugin) { this.plugin = plugin; }

    @Override
    public synchronized void register(Listener listener)
    {
        Objects.requireNonNull(listener, "listener");
        if (registeredListeners.contains(listener))
        {
            throw new IllegalStateException("Listener is already registered");
        }
        plugin.getServer().getPluginManager().registerEvents(listener, plugin);
        registeredListeners.add(listener);
    }

    @Override
    public synchronized void register(Listener listener, EventRule<?>... rules)
    {
        Objects.requireNonNull(listener, "listener");
        Objects.requireNonNull(rules, "rules");
        Set<EventRule<?>> newRules = new HashSet<>();
        Set<EventRule<?>> existingRules = registeredRules.get(listener);
        for (EventRule<?> rule : rules)
        {
            Objects.requireNonNull(rule, "rule");
            if (!newRules.add(rule) || existingRules != null && existingRules.contains(rule))
            {
                throw new IllegalStateException("Event rule is already registered for this listener");
            }
        }
        for (EventRule<?> rule : rules)
        {
            registerRule(listener, rule);
            registeredRules.computeIfAbsent(listener, ignored -> new HashSet<>()).add(rule);
        }
    }

    @Override
    public synchronized Listener register(EventRule<?>... rules)
    {
        Listener listener = new Listener()
        {
        };
        register(listener, rules);
        return listener;
    }

    private <E extends Event> void registerRule(Listener listener, EventRule<E> rule)
    {
        plugin.getServer().getPluginManager().registerEvent(rule.eventType(), listener, rule.priority(),
                (registeredListener, event) ->
                {
                    if (rule.eventType().isInstance(event))
                    {
                        rule.handler().accept(rule.eventType().cast(event));
                    }
                }, plugin, rule.ignoreCancelled());
    }

    @Override
    public synchronized void unregister(Listener listener)
    {
        Objects.requireNonNull(listener, "listener");
        HandlerList.unregisterAll(listener);
        registeredListeners.remove(listener);
        registeredRules.remove(listener);
    }
}
