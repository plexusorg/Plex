package dev.plex.listener.impl;

import dev.plex.Plex;
import dev.plex.api.listener.EventRule;
import dev.plex.listener.ServerListenerBase;
import dev.plex.services.impl.TimingService;
import dev.plex.util.PlexUtils;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

public class AntiNukerListener extends ServerListenerBase
{
    public AntiNukerListener(Plex plugin)
    {
        super(plugin);
        plugin.getApi().listeners().register(this,
                EventRule.of(BlockPlaceEvent.class, EventPriority.HIGH, event -> checkForNuker(event.getPlayer(), event)),
                EventRule.of(BlockBreakEvent.class, EventPriority.HIGH, event -> checkForNuker(event.getPlayer(), event)));
    }

    private void checkForNuker(Player player, Cancellable event)
    {
        UUID uuid = player.getUniqueId();
        TimingService.nukerCooldown.merge(uuid, 1L, Long::sum);
        if (getCount(uuid) > 200L)
        {
            TimingService.strikes.merge(uuid, 1L, Long::sum);
            player.kick(PlexUtils.messageComponent("nukerKickMessage"));
            event.setCancelled(true);
        }
    }

    public long getCount(UUID uuid)
    {
        return TimingService.nukerCooldown.getOrDefault(uuid, 1L);
    }
}
