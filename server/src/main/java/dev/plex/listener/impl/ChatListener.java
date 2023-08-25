package dev.plex.listener.impl;

import dev.plex.listener.PlexListener;
import dev.plex.listener.annotation.Toggleable;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexUtils;
import dev.plex.util.minimessage.SafeMiniMessage;
import dev.plex.util.redis.MessageUtil;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

@Toggleable("chat.enabled")
public class ChatListener extends PlexListener
{
    public static final TextReplacementConfig URL_REPLACEMENT_CONFIG = TextReplacementConfig
            .builder()
            .match("(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")
            .replacement((matchResult, builder) -> Component.empty()
                    .content(matchResult.group())
                    .clickEvent(ClickEvent.openUrl(
                            matchResult.group()
                    ))).build();
    public static BiConsumer<AsyncChatEvent, PlexPlayer> PRE_RENDERER = ChatListener::defaultChatProcessing;
    private final PlexChatRenderer renderer = new PlexChatRenderer();

    @EventHandler
    public void onChat(AsyncChatEvent event)
    {
        PlexPlayer plexPlayer = plugin.getPlayerCache().getPlexPlayerMap().get(event.getPlayer().getUniqueId());
        if (plexPlayer.isStaffChat())
        {
            MessageUtil.sendStaffChat(event.getPlayer(), event.message(), PlexUtils.adminChat(event.getPlayer().getName(), SafeMiniMessage.mmSerialize(event.message())).toArray(UUID[]::new));
            plugin.getServer().getConsoleSender().sendMessage(PlexUtils.messageComponent("adminChatFormat", event.getPlayer().getName(), SafeMiniMessage.mmSerialize(event.message())).replaceText(URL_REPLACEMENT_CONFIG));
            event.setCancelled(true);
            return;
        }
        Component prefix = plugin.getRankManager().getPrefix(plexPlayer);

        if (prefix != null && !prefix.equals(Component.empty()) && !prefix.equals(Component.space()))
        {
            renderer.hasPrefix = true;
            renderer.prefix = prefix;
        }
        else
        {
            renderer.hasPrefix = false;
            renderer.prefix = null;
        }

        PRE_RENDERER.accept(event, plexPlayer);

        event.renderer(renderer);
    }

    public static class PlexChatRenderer implements ChatRenderer
    {
        public boolean hasPrefix;
        public Component prefix;
        public Supplier<Component> before = null;

        @Override
        public @NotNull Component render(@NotNull Player source, @NotNull Component sourceDisplayName, @NotNull Component message, @NotNull Audience viewer)
        {
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
                    .append(message)
                    .replaceText(URL_REPLACEMENT_CONFIG);
        }
    }

    private static void defaultChatProcessing(AsyncChatEvent event, PlexPlayer plexPlayer)
    {
        String text = PlexUtils.cleanString(PlexUtils.getTextFromComponent(event.message()));
        event.message(SafeMiniMessage.mmDeserializeWithoutEvents(text));
    }
}
