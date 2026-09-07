package dev.plex.api.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.command.CommandSender;

/**
 * Formats configured messages and broadcasts Adventure components.
 */
public interface MessageApi
{
    /**
     * Resolves a configured message entry into a component.
     *
     * @param entry message key
     * @param placeholders named replacement values
     * @return resolved component
     */
    Component messageComponent(String entry, TagResolver... placeholders);

    /**
     * Gets the raw configured MiniMessage template without resolving tags.
     *
     * @param entry message key
     * @return raw message template
     */
    String messageString(String entry);

    /**
     * Converts MiniMessage text to a component.
     *
     * @param input MiniMessage input
     * @param placeholders dynamic MiniMessage tags
     * @return message component
     */
    Component miniMessage(String input, TagResolver... placeholders);

    /**
     * Broadcasts a MiniMessage string to online players.
     *
     * @param miniMessage MiniMessage input to broadcast
     */
    void broadcast(String miniMessage);

    /**
     * Broadcasts a component to online players.
     *
     * @param component component to broadcast
     */
    void broadcast(Component component);

    /**
     * Captures recipient policy for an administrative action on the sender's command thread,
     * before starting asynchronous work or scheduling work on another entity or region.
     * A vanished player's announcements go only to that player, console, and online players
     * the active SuperVanish/PremiumVanish integration allows to see them at capture time.
     * Later joins, disconnects, or visibility changes do not expand or refresh that audience.
     * Non-player senders and nonvanished players retain ordinary public broadcast behavior.
     * Without a supported vanish plugin, announcements are public.
     *
     * @param sender player or non-player performing the action
     * @return reusable announcement delivery for this action, not for subsequent invocations
     */
    ActionBroadcast captureActionBroadcast(CommandSender sender);

}
