package dev.plex.listener.impl;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import org.bukkit.Bukkit;
import org.bukkit.Sound;

import dev.plex.Plex;
import dev.plex.api.event.StaffChatMessageEvent;
import dev.plex.hook.VaultHook;
import dev.plex.listener.ServerListenerBase;
import dev.plex.meta.PlayerMeta;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexUtils;
import dev.plex.util.redis.MessageUtil;
import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;

import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
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
            .match("https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")
            .replacement((matchResult, builder) -> Component.empty()
                    .content(matchResult.group())
                    .clickEvent(ClickEvent.openUrl(
                            matchResult.group()
                    ))).build();
    public static BiConsumer<AsyncChatEvent, PlexPlayer> PRE_RENDERER = ChatListener::defaultChatProcessing;

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent event)
    {
        PlexPlayer plexPlayer = plugin.getPlayerService().cachedPlayer(event.getPlayer().getUniqueId());
        PlexChatRenderer renderer = new PlexChatRenderer();
        renderer.format = plugin.config.getString("chat.format");
        PRE_RENDERER.accept(event, plexPlayer);
        if (plexPlayer.isStaffChat())
        {
            Component prefix = VaultHook.getPrefix(event.getPlayer()); // Staff chat uses the group prefix, not a player's custom prefix.
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
            Runnable broadcast = () ->
            {
                MessageUtil.sendStaffChat(plugin, event.getPlayer(), message, PlexUtils.adminChat(event.getPlayer().getName(), prefix, message).toArray(UUID[]::new));
                plugin.getServer().getConsoleSender().sendMessage(PlexUtils.messageComponent("adminChatFormat", Placeholder.unparsed("sender", event.getPlayer().getName()), Placeholder.component("prefix", prefix), Placeholder.component("message", message.replaceText(URL_REPLACEMENT_CONFIG))));
            };
            if (event.isAsynchronous()) Bukkit.getGlobalRegionScheduler().run(plugin, task -> broadcast.run());
            else broadcast.run();
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

        boolean nicknameHover = plugin.config.getBoolean("chat.nickname-hover", true);
        boolean mentions = plugin.config.getBoolean("chat.mentions", true);
        event.renderer((source, displayName, message, viewer) ->
        {
            if (nicknameHover && !PlexUtils.getTextFromComponent(displayName).equals(source.getName()))
            {
                displayName = displayName.hoverEvent(HoverEvent.showText(Component.text(source.getName())));
            }
            if (mentions && viewer instanceof Player recipient && !recipient.getUniqueId().equals(source.getUniqueId()))
            {
                Pattern username = Pattern.compile("(?<![a-zA-Z0-9_])" + Pattern.quote(recipient.getName()) + "(?![a-zA-Z0-9_])", Pattern.CASE_INSENSITIVE);
                if (username.matcher(PlexUtils.getTextFromComponent(message)).find())
                {
                    message = message.replaceText(TextReplacementConfig.builder()
                            .match(username)
                            .replaceInsideHoverEvents(false)
                            .replacement((match, text) -> text.color(NamedTextColor.YELLOW))
                            .build());
                    // Paper renders once per recipient. Only the sound needs entity-owned state.
                    recipient.getScheduler().run(plugin, task -> recipient.playSound(recipient.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f), null);
                }
            }
            return renderer.render(source, displayName, message);
        });
    }

    public static class PlexChatRenderer implements ChatRenderer.ViewerUnaware
    {
        public boolean hasPrefix;
        public Component prefix;
        public String format;
        public Supplier<Component> before = null;

        @Override
        public @NotNull Component render(@NotNull Player source, @NotNull Component sourceDisplayName, @NotNull Component message)
        {
            Component renderedPrefix = hasPrefix && prefix != null ? prefix : Component.empty();
            Component component = PlexUtils.mmDeserialize(format,
                    Placeholder.component("prefix", renderedPrefix),
                    Placeholder.unparsed("name", source.getName()),
                    Placeholder.component("displayname", sourceDisplayName),
                    // UUID.toString() is safe for native preprocessing, including <head:<uuid>>.
                    Placeholder.parsed("uuid", source.getUniqueId().toString()),
                    Placeholder.component("message", message));

            if (before != null)
            {
                component = component.append(before.get());
            }

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
