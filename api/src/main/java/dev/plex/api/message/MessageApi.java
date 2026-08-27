package dev.plex.api.message;

import java.util.List;
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
        return messageComponent(entry, new Object[0]);
    }

    /**
     * Resolves a configured message entry into a component.
     *
     * @param entry message key
     * @param objects replacement values
     * @return resolved component
     */
    Component messageComponent(String entry, Object... objects);

    /**
     * Resolves a configured message entry into a component using component replacements.
     *
     * @param entry message key
     * @param objects component replacement values
     * @return resolved component
     */
    Component messageComponent(String entry, Component... objects);

    /**
     * Resolves a configured message entry into a plain string.
     *
     * @param entry message key
     * @param objects replacement values
     * @return resolved message string
     */
    String messageString(String entry, Object... objects);

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

    /**
     * Returns the names of online players.
     *
     * @return names of online players
     */
    List<String> onlinePlayerNames();
}
