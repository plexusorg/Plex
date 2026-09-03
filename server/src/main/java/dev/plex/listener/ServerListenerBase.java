package dev.plex.listener;

import dev.plex.Plex;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;

public abstract class ServerListenerBase implements Listener
{
    protected final Plex plugin;

    protected ServerListenerBase(Plex plugin)
    {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @SafeVarargs
    protected final void registerRules(EventRule<? extends Event>... rules)
    {
        for (EventRule<? extends Event> rule : rules) registerRule(rule);
    }

    private <E extends Event> void registerRule(EventRule<E> rule)
    {
        plugin.getServer().getPluginManager().registerEvent(rule.eventType(), this, rule.priority(),
                (listener, event) ->
                {
                    if (rule.eventType().isInstance(event)) rule.handler().accept(rule.eventType().cast(event));
                }, plugin, rule.ignoreCancelled());
    }
}
