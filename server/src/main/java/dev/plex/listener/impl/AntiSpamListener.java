package dev.plex.listener.impl;

import dev.plex.Plex;
import dev.plex.abuse.AbuseTracker;
import dev.plex.api.listener.EventRule;
import dev.plex.listener.ServerListenerBase;
import dev.plex.util.PlexUtils;
import io.papermc.paper.event.player.AsyncChatEvent;

import java.time.Duration;
import java.util.UUID;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class AntiSpamListener extends ServerListenerBase
{
    private static final AbuseTracker TRACKER = new AbuseTracker(
            Duration.ofSeconds(5), 8, 0, Duration.ofMinutes(30), 10_000);

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
        AbuseTracker.Decision decision = TRACKER.record(uuid);
        if (!decision.allowed())
        {
            event.setCancelled(true);
            if (decision.thresholdCrossed())
            {
                player.sendMessage(PlexUtils.messageComponent("antiSpamMessage"));
            }
        }
    }
}
