package dev.plex.listener.impl;

import dev.plex.Plex;
import dev.plex.api.listener.EventRule;
import dev.plex.listener.ServerListenerBase;
import dev.plex.services.impl.TimingService;
import dev.plex.util.PlexUtils;
import io.papermc.paper.event.player.AsyncChatEvent;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class AntiSpamListener extends ServerListenerBase
{
    public AntiSpamListener(Plex plugin)
    {
        super(plugin);
        plugin.getApi().listeners().register(this,
                EventRule.of(AsyncChatEvent.class, EventPriority.NORMAL, event -> checkForSpam(event.getPlayer(), event)),
                EventRule.of(PlayerCommandPreprocessEvent.class, EventPriority.HIGHEST, event -> checkForSpam(event.getPlayer(), event)));
    }

    private void checkForSpam(Player player, Cancellable event)
    {
        UUID uuid = player.getUniqueId();
        TimingService.spamCooldown.merge(uuid, 1L, Long::sum);
        if (getCount(uuid) > 8L)
        {
            player.sendMessage(PlexUtils.messageComponent("antiSpamMessage"));
            event.setCancelled(true);
        }
    }

    public long getCount(UUID uuid)
    {
        return TimingService.spamCooldown.getOrDefault(uuid, 1L);
    }
}
