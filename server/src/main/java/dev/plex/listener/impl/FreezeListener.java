package dev.plex.listener.impl;

import dev.plex.Plex;

import dev.plex.listener.EventRule;
import dev.plex.listener.ServerListenerBase;
import dev.plex.player.PlexPlayer;
import java.util.UUID;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

public class FreezeListener extends ServerListenerBase
{
    public FreezeListener(Plex plugin)
    {
        super(plugin);
        registerRules(
                EventRule.blocking(PlayerMoveEvent.class, EventPriority.NORMAL, event -> isFrozen(event.getPlayer().getUniqueId())),
                EventRule.blocking(PlayerTeleportEvent.class, EventPriority.NORMAL, event -> isFrozen(event.getPlayer().getUniqueId())));
    }

    private boolean isFrozen(UUID uuid)
    {
        PlexPlayer plexPlayer = plugin.getPlayerService().cachedPlayer(uuid);
        return plexPlayer != null && plexPlayer.isFrozen();
    }
}
