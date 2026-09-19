package dev.plex.api.impl;

import dev.plex.Plex;
import dev.plex.api.message.ActionBroadcast;
import dev.plex.api.message.MessageApi;
import dev.plex.listener.impl.ChatListener;
import dev.plex.util.CapturedActionBroadcast;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.Component;

final class DefaultMessageApi implements MessageApi
{
    private final Plex plugin;

    DefaultMessageApi(Plex plugin) { this.plugin = plugin; }

    @Override public Component messageComponent(String entry, TagResolver... placeholders) { return PlexUtils.messageComponent(entry, placeholders); }
    @Override public String messageString(String entry) { return PlexUtils.messageString(entry); }
    @Override public Component miniMessage(String input, TagResolver... placeholders) { return PlexUtils.mmDeserialize(input, placeholders); }
    @Override public Component playerText(String input) { return PlexUtils.stringToComponent(input); }
    @Override public Component chatLine(Player player, Component message) { return ChatListener.PlexChatRenderer.forPlayer(plugin, plugin.getPlayerService().cachedPlayer(player.getUniqueId())).render(player, player.displayName(), message); }
    @Override public void broadcast(String miniMessage) { PlexUtils.broadcast(miniMessage); }
    @Override public void broadcast(Component component) { PlexUtils.broadcast(component); }
    @Override public ActionBroadcast captureActionBroadcast(CommandSender sender) { return CapturedActionBroadcast.capture(sender); }
}
