package dev.plex.api.impl;

import dev.plex.api.message.MessageApi;
import dev.plex.util.PlexUtils;
import java.util.List;
import net.kyori.adventure.text.Component;

final class DefaultMessageApi implements MessageApi
{
    @Override public Component messageComponent(String entry, Object... objects) { return PlexUtils.messageComponent(entry, objects); }
    @Override public Component messageComponent(String entry, Component... objects) { return PlexUtils.messageComponent(entry, objects); }
    @Override public String messageString(String entry, Object... objects) { return PlexUtils.messageString(entry, objects); }
    @Override public Component miniMessage(String input) { return PlexUtils.mmDeserialize(input); }
    @Override public void broadcast(String miniMessage) { PlexUtils.broadcast(miniMessage); }
    @Override public void broadcast(Component component) { PlexUtils.broadcast(component); }
    @Override public List<String> onlinePlayerNames() { return PlexUtils.getPlayerNameList(); }
}
