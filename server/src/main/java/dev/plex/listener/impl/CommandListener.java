package dev.plex.listener.impl;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import dev.plex.Plex;
import dev.plex.listener.ServerListenerBase;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandListener extends ServerListenerBase
{
    public CommandListener(Plex plugin)
    {
        super(plugin);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event)
    {
        Player sender = event.getPlayer();
        PlexLog.debug("Command monitor for {0}: cancelled={1}", sender.getUniqueId(), event.isCancelled());
        if (event.isCancelled()) return;
        String senderName = sender.getName();
        Component command = Component.text(event.getMessage()).replaceText(ChatListener.URL_REPLACEMENT_CONFIG);
        plugin.getPlayerService().cachedPlayers().forEach(plexPlayer ->
        {
            if (!plexPlayer.isCommandSpy() || plexPlayer.getUuid().equals(sender.getUniqueId())) return;
            Player recipient = Bukkit.getPlayer(plexPlayer.getUuid());
            if (recipient == null) return;
            if (recipient.hasPermission("plex.commandspy"))
            {
                recipient.sendMessage(PlexUtils.messageComponent("commandSpyFormat", Placeholder.component("sender", Component.text(senderName)), Placeholder.component("command", command)));
            }
        });
    }
}
