package dev.plex.api.message;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;

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

}
