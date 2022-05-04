package dev.plex.listener.impl;

import dev.plex.cache.PlayerCache;
import dev.plex.listener.PlexListener;
import dev.plex.listener.annotation.Toggleable;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexUtils;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.jetbrains.annotations.NotNull;

@Toggleable("chat.enabled")
public class ChatListener extends PlexListener
{
    private final static TextReplacementConfig URL_REPLACEMENT_CONFIG = TextReplacementConfig
            .builder()
            .match("(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")
            .replacement((matchResult, builder) -> Component.empty()
                    .content(matchResult.group())
                    .clickEvent(ClickEvent.openUrl(
                            matchResult.group()
                    ))).build();
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
            message = message.replaceText(URL_REPLACEMENT_CONFIG);

            if (hasPrefix)
            {
                return Component.empty()
                        .append(prefix)
                        .append(Component.space())
                        .append(PlexUtils.mmDeserialize(plugin.config.getString("chat.name-color", "<white>") + MiniMessage.builder().tags(TagResolver.resolver(StandardTags.color(), StandardTags.rainbow(), StandardTags.decorations(), StandardTags.gradient(), StandardTags.transition())).build().serialize(sourceDisplayName)))
                        .append(Component.space())
                        .append(Component.text("»").color(NamedTextColor.GRAY))
                        .append(Component.space())
                        .append(message);
            }
            return Component.empty()
                    .append(PlexUtils.mmDeserialize(plugin.config.getString("chat.name-color", "<white>") + MiniMessage.builder().tags(TagResolver.resolver(StandardTags.color(), StandardTags.rainbow(), StandardTags.decorations(), StandardTags.gradient(), StandardTags.transition())).build().serialize(sourceDisplayName)))
                    .append(Component.space())
                    .append(Component.text("»").color(NamedTextColor.GRAY))
                    .append(Component.space())
                    .append(message);
        }
    }
}
