package dev.plex.listener.impl;

import dev.plex.Plex;
import dev.plex.cache.PlayerCache;
import dev.plex.listener.PlexListener;
import dev.plex.player.PlexPlayer;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.jetbrains.annotations.NotNull;

public class ChatListener extends PlexListener
{

    private final PlexChatRenderer renderer = new PlexChatRenderer();

    @EventHandler
    public void onChat(AsyncChatEvent event)
    {
        PlexPlayer plexPlayer = PlayerCache.getPlexPlayerMap().get(event.getPlayer().getUniqueId());

        if (!plexPlayer.getPrefix().isEmpty())
        {
            renderer.hasPrefix = true;
            renderer.prefix = plexPlayer.getPrefix();
        } else if (Plex.get().getRankManager().isAdmin(plexPlayer))
        {
            renderer.hasPrefix = true;
            renderer.prefix = plexPlayer.getRankFromString().getPrefix();
        }
        event.renderer(renderer);
        /*if (!plexPlayer.getPrefix().isEmpty())
        {
            event.setFormat(String.format("%s %s §7» %s", plexPlayer.getPrefix(), ChatColor.RESET + plexPlayer.displayName(), event.getMessage()));
        } else if (Plex.get().getRankManager().isAdmin(plexPlayer))
        {
            event.setFormat(String.format("%s %s §7» %s", plexPlayer.getRankFromString().getPrefix(), ChatColor.RESET + plexPlayer.displayName(), event.getMessage()));
        } else
        {
            event.setFormat(String.format("%s §7» %s", ChatColor.RESET + plexPlayer.displayName(), event.getMessage()));
        }*/
    }

    public static class PlexChatRenderer implements ChatRenderer
    {
        public boolean hasPrefix;
        public String prefix;

        @Override
        public @NotNull Component render(@NotNull Player source, @NotNull Component sourceDisplayName, @NotNull Component message, @NotNull Audience viewer)
        {
            if (hasPrefix)
            {
                return LegacyComponentSerializer.legacyAmpersand().deserialize(prefix)
                        .append(Component.space())
                        .append(sourceDisplayName)
                        .append(Component.space())
                        .append(Component.text("»").color(NamedTextColor.GRAY))
                        .append(Component.space())
                        .append(message);
            }
            return Component.empty()
                    .append(sourceDisplayName)
                    .append(Component.space())
                    .append(Component.text("»").color(NamedTextColor.GRAY))
                    .append(Component.space())
                    .append(message);
        }
    }
}
