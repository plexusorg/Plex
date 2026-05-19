package dev.plex.api.message;

import java.util.List;
import net.kyori.adventure.text.Component;

public interface MessageApi
{
    Component messageComponent(String entry, Object... objects);
    Component messageComponent(String entry, Component... objects);
    String messageString(String entry, Object... objects);
    Component miniMessage(String input);
    void broadcast(String miniMessage);
    void broadcast(Component component);
    List<String> onlinePlayerNames();
}
