package dev.plex.api.impl;

import dev.plex.Plex;
import dev.plex.api.listener.EventRule;
import dev.plex.api.listener.ListenerApi;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

import java.util.Objects;

final class DefaultListenerApi implements ListenerApi
{
    private final Plex plugin;

    DefaultListenerApi(Plex plugin) { this.plugin = plugin; }

    @Override
    public void register(Listener listener)
    {
        plugin.getServer().getPluginManager().registerEvents(Objects.requireNonNull(listener, "listener"), plugin);
    }

    @Override
    public void register(Listener listener, EventRule<?>... rules)
    {
        Objects.requireNonNull(listener, "listener");
        Objects.requireNonNull(rules, "rules");
        for (EventRule<?> rule : rules)
        {
            register(listener, Objects.requireNonNull(rule, "rule"));
        }
    }

    @Override
    public Listener register(EventRule<?>... rules)
    {
        Listener listener = new Listener()
        {
        };
        register(listener, rules);
        return listener;
    }

    private <E extends Event> void register(Listener listener, EventRule<E> rule)
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
    public void unregister(Listener listener)
    {
        HandlerList.unregisterAll(Objects.requireNonNull(listener, "listener"));
    }
}
