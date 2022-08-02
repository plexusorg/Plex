package dev.plex.listener.impl;

import dev.plex.api.chat.IChatHandler;
import dev.plex.listener.PlexListener;
import dev.plex.listener.annotation.Toggleable;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexUtils;
import dev.plex.util.minimessage.SafeMiniMessage;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.function.Supplier;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
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

    @EventHandler
    public void onChat(AsyncChatEvent event)
    {
        plugin.getChatHandler().doChat(event);
    }

    public static class ChatHandlerImpl implements IChatHandler
    {
        private final PlexChatRenderer renderer = new PlexChatRenderer();

        @Override
        public void doChat(AsyncChatEvent event)
        {
            PlexPlayer plexPlayer = plugin.getPlayerCache().getPlexPlayerMap().get(event.getPlayer().getUniqueId());
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
    }

    public static class PlexChatRenderer implements ChatRenderer
    {
        public boolean hasPrefix;
        public Component prefix;
        public Supplier<Component> before = null;

        @Override
        public @NotNull Component render(@NotNull Player source, @NotNull Component sourceDisplayName, @NotNull Component message, @NotNull Audience viewer)
        {
            String text = PlexUtils.cleanString(PlexUtils.getTextFromComponent(message));

            Component component = Component.empty();

            if (before != null)
            {
                component = component.append(before.get());
            }
            if (hasPrefix)
            {
                component = component.append(prefix).append(Component.space());
            }
            return component
                    .append(Component.empty())
                    .append(
                            source.name().equals(sourceDisplayName) ?
                                    SafeMiniMessage.mmDeserialize(plugin.config.getString("chat.name-color") + SafeMiniMessage.mmSerialize(sourceDisplayName))
                                    : SafeMiniMessage.mmDeserialize(plugin.config.getString("chat.name-color")).append(sourceDisplayName)
                    )
                    .append(Component.space())
                    .append(Component.text("»").color(NamedTextColor.GRAY))
                    .append(Component.space())
                    .append(SafeMiniMessage.mmDeserializeWithoutEvents(text))
                    .replaceText(URL_REPLACEMENT_CONFIG);
        }
    }
}
