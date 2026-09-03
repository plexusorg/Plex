package dev.plex.listener.impl;

import dev.plex.Plex;
import dev.plex.listener.ServerListenerBase;
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event)
    {
        Player sender = event.getPlayer();
        String senderName = sender.getName();
        String command = event.getMessage();
        plugin.getPlayerService().cachedPlayers().forEach(plexPlayer ->
        {
            if (!plexPlayer.isCommandSpy() || plexPlayer.getUuid().equals(sender.getUniqueId())) return;
            Player recipient = Bukkit.getPlayer(plexPlayer.getUuid());
            if (recipient == null) return;
            if (recipient.hasPermission("plex.commandspy"))
            {
                recipient.sendMessage(PlexUtils.messageComponent("commandSpyFormat",
                        Component.text(senderName), Component.text(command)));
            }
        });
    }
}
