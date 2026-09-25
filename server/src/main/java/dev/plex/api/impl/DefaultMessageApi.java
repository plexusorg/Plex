package dev.plex.api.impl;

import dev.plex.Plex;
import dev.plex.api.event.StaffChatMessageEvent;
import dev.plex.api.message.ActionBroadcast;
import dev.plex.api.message.MessageApi;
import dev.plex.listener.impl.ChatListener;
import dev.plex.util.CapturedActionBroadcast;
import org.bukkit.Bukkit;
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
    @Override public Component chatLine(Player player, Component message) { return ChatListener.PlexChatRenderer.forPlayer(plugin, player, plugin.getPlayerService().cachedPlayer(player.getUniqueId()), false).render(player, player.displayName(), message); }
    @Override public void broadcast(String miniMessage) { PlexUtils.broadcast(miniMessage); }
    @Override public void broadcast(Component component) { PlexUtils.broadcast(component); }
    @Override public ActionBroadcast captureActionBroadcast(CommandSender sender) { return CapturedActionBroadcast.capture(sender); }
    @Override public void sendAdminChat(String senderName, Component prefix, Component message) {
        StaffChatMessageEvent staffChatEvent = new StaffChatMessageEvent(
                message,
                StaffChatMessageEvent.Source.API,
                !Bukkit.isPrimaryThread());
        plugin.getServer().getPluginManager().callEvent(staffChatEvent);

        if (staffChatEvent.isCancelled()) return;

        PlexUtils.adminChat(senderName, prefix, message);
    }
}
