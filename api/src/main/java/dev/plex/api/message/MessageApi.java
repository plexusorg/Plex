package dev.plex.api.message;

import net.kyori.adventure.text.Component;

/**
 * Formats configured messages and broadcasts Adventure components.
 */
public interface MessageApi
{
    /**
     * Gets a configured message as a component.
     *
     * @param entry message key
     * @return message component
     */
    default Component messageComponent(String entry)
    {
        return messageComponent(entry, new MessagePlaceholder[0]);
    }

    /**
     * Resolves a configured message entry into a component.
     *
     * @param entry message key
     * @param placeholders named replacement values
     * @return resolved component
     */
    Component messageComponent(String entry, MessagePlaceholder... placeholders);

    /**
     * Resolves a configured message entry into a plain string.
     *
     * @param entry message key
     * @param placeholders named replacement values
     * @return resolved message string
     */
    String messageString(String entry, MessagePlaceholder... placeholders);

    /**
     * Converts MiniMessage text to a component.
     *
     * @param input MiniMessage input
     * @return message component
     */
    Component miniMessage(String input);

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
