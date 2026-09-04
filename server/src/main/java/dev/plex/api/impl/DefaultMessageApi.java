package dev.plex.api.impl;

import dev.plex.api.message.MessageApi;
import dev.plex.api.message.MessagePlaceholder;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.Component;

final class DefaultMessageApi implements MessageApi
{
    @Override public Component messageComponent(String entry, MessagePlaceholder... placeholders) { return PlexUtils.messageComponent(entry, placeholders); }
    @Override public String messageString(String entry, MessagePlaceholder... placeholders) { return PlexUtils.messageString(entry, placeholders); }
    @Override public Component miniMessage(String input) { return PlexUtils.mmDeserialize(input); }
    @Override public void broadcast(String miniMessage) { PlexUtils.broadcast(miniMessage); }
    @Override public void broadcast(Component component) { PlexUtils.broadcast(component); }
}
