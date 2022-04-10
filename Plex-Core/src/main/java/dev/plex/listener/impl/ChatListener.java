package dev.plex.listener.impl;

import dev.plex.cache.player.PlayerCache;
import dev.plex.listener.PlexListener;
import dev.plex.listener.annotation.Toggleable;
import dev.plex.player.PlexPlayer;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.apache.commons.lang.StringUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.jetbrains.annotations.NotNull;

@Toggleable("chat.enabled")
public class ChatListener extends PlexListener
{
    private final PlexChatRenderer renderer = new PlexChatRenderer();

    @EventHandler
    public void onChat(AsyncChatEvent event)
    {
        PlexPlayer plexPlayer = PlayerCache.getPlexPlayerMap().get(event.getPlayer().getUniqueId());

        Component prefix = plugin.getRankManager().getPrefix(plexPlayer);
        if (prefix != null)
        {
            renderer.hasPrefix = true;
            renderer.prefix = prefix;
        }
        else
        {
            renderer.hasPrefix = false;
            renderer.prefix = null;
        }
        event.renderer(renderer);
    }

    public static class PlexChatRenderer implements ChatRenderer
    {
        public boolean hasPrefix;
        public Component prefix;

        @Override
        public @NotNull Component render(@NotNull Player source, @NotNull Component sourceDisplayName, @NotNull Component message, @NotNull Audience viewer)
        {
            if (hasPrefix)
            {
                return Component.empty()
                        .append(prefix)
                        .append(Component.space())
                        .append(MiniMessage.miniMessage().deserialize(plugin.config.getString("chat.name-color", "<white>"))).append(sourceDisplayName)
                        .append(Component.space())
                        .append(Component.text("»").color(NamedTextColor.GRAY))
                        .append(Component.space())
                        .append(message);
            }
            return Component.empty()
                    .append(MiniMessage.miniMessage().deserialize(plugin.config.getString("chat.name-color", "<white>"))).append(sourceDisplayName)
                    .append(Component.space())
                    .append(Component.text("»").color(NamedTextColor.GRAY))
                    .append(Component.space())
                    .append(message);
        }
    }
}
