package dev.plex.listener.impl;

import dev.plex.Plex;
import dev.plex.api.event.StaffChatMessageEvent;
import dev.plex.hook.VaultHook;
import dev.plex.listener.ServerListenerBase;
import dev.plex.meta.PlayerMeta;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexUtils;
import dev.plex.util.minimessage.SafeMiniMessage;
import dev.plex.util.redis.MessageUtil;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.NotNull;

public class ChatListener extends ServerListenerBase
{
    public ChatListener(Plex plugin)
    {
        super(plugin);
    }

    public static final TextReplacementConfig URL_REPLACEMENT_CONFIG = TextReplacementConfig
            .builder()
            .match("(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")
            .replacement((matchResult, builder) -> Component.empty()
                    .content(matchResult.group())
                    .clickEvent(ClickEvent.openUrl(
                            matchResult.group()
                    ))).build();
    public static BiConsumer<AsyncChatEvent, PlexPlayer> PRE_RENDERER = ChatListener::defaultChatProcessing;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event)
    {
        PlexPlayer plexPlayer = plugin.getPlayerCache().getPlexPlayerMap().get(event.getPlayer().getUniqueId());
        PlexChatRenderer renderer = new PlexChatRenderer();
        renderer.format = SafeMiniMessage.mmDeserialize(plugin.config.getString("chat.format"));
        if (plexPlayer.isStaffChat())
        {
            String prefix = PlexUtils.mmSerialize(VaultHook.getPrefix(event.getPlayer())); // Don't use PlexPlayer#getPrefix because that returns their custom set prefix and not their group's
            StaffChatMessageEvent staffChatEvent = new StaffChatMessageEvent(
                    event.getPlayer(),
                    event.message(),
                    StaffChatMessageEvent.Source.TOGGLED_CHAT,
                    event.isAsynchronous());
            plugin.getServer().getPluginManager().callEvent(staffChatEvent);
            event.setCancelled(true);
            if (staffChatEvent.isCancelled())
            {
                return;
            }
            Component message = staffChatEvent.getMessage();
            MessageUtil.sendStaffChat(plugin, event.getPlayer(), message, PlexUtils.adminChat(event.getPlayer().getName(), prefix, SafeMiniMessage.mmSerialize(message)).toArray(UUID[]::new));
            plugin.getServer().getConsoleSender().sendMessage(PlexUtils.messageComponent("adminChatFormat", event.getPlayer().getName(), prefix, SafeMiniMessage.mmSerialize(message.replaceText(URL_REPLACEMENT_CONFIG))));
            return;
        }
        Component prefix = PlayerMeta.getPrefix(plexPlayer);

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
        public Component format;
        public Supplier<Component> before = null;

        @Override
        public @NotNull Component render(@NotNull Player source, @NotNull Component sourceDisplayName, @NotNull Component message, @NotNull Audience viewer)
        {
            Component component = format;

            if (before != null)
            {
                component = component.append(before.get());
            }

            // Substitute the prefix from the config
            if (hasPrefix)
            {
                component = component.replaceText(TextReplacementConfig.builder().matchLiteral("{prefix}").replacement(prefix).build());
            }

            // Substitute the display name from the config
            component = component.replaceText(TextReplacementConfig.builder().matchLiteral("{name}")
                    .replacement(sourceDisplayName).build());

            // Substitute the message from the config
            component = component.replaceText(TextReplacementConfig.builder().matchLiteral("{message}").replacement(message).build());

            // Fix links not being clickable
            component = component.replaceText(URL_REPLACEMENT_CONFIG);

            return component;
        }
    }

    private static void defaultChatProcessing(AsyncChatEvent event, PlexPlayer plexPlayer)
    {
        String text = PlexUtils.getTextFromComponent(event.message());
        event.message(PlexUtils.stringToComponent(text));
    }
}
