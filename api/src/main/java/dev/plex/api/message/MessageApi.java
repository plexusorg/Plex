package dev.plex.api.message;

import java.util.List;
import net.kyori.adventure.text.Component;

/**
 * Formats configured messages and broadcasts Adventure components.
 */
public interface MessageApi
{
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
     * Deserializes MiniMessage input into a component.
     *
     * @param input MiniMessage input
     * @return deserialized component
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
     * Returns the names of currently online players.
     *
     * @return names of currently online players
     */
    List<String> onlinePlayerNames();
}
