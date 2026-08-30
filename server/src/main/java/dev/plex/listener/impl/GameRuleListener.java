package dev.plex.listener.impl;

import dev.plex.Plex;
import dev.plex.listener.ServerListenerBase;
import dev.plex.util.GameRuleUtil;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.world.WorldLoadEvent;

public final class GameRuleListener extends ServerListenerBase
{
    public GameRuleListener(Plex plugin)
    {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldLoad(WorldLoadEvent event)
    {
        GameRuleUtil.apply(plugin, event.getWorld());
    }
}
