package dev.plex.listener.impl;

import dev.plex.listener.PlexListener;
import dev.plex.services.impl.TimingService;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.UUID;

public class AntiSpamListener extends PlexListener
{
    @EventHandler
    public void onChat(AsyncChatEvent event)
    {
        TimingService.spamCooldown.merge(event.getPlayer().getUniqueId(), 1L, Long::sum);
        if (getCount(event.getPlayer().getUniqueId()) > 8L)
        {
            event.getPlayer().sendMessage(Component.text("Please refrain from spamming messages.").color(NamedTextColor.GRAY));
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event)
    {
        TimingService.spamCooldown.merge(event.getPlayer().getUniqueId(), 1L, Long::sum);
        if (getCount(event.getPlayer().getUniqueId()) > 8L)
        {
            event.getPlayer().sendMessage(Component.text("Please refrain from spamming commands.").color(NamedTextColor.GRAY));
            event.setCancelled(true);
        }
    }

    public long getCount(UUID uuid)
    {
        return TimingService.spamCooldown.getOrDefault(uuid, 1L);
    }
}
