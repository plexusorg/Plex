package dev.plex.listener.impl;

import dev.plex.listener.PlexListener;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.List;

public class MuteListener extends PlexListener
{
    List<String> commands = plugin.commands.getStringList("block_on_mute");

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event)
    {
        if (plugin.getPlayerCache().getPlexPlayer(event.getPlayer().getUniqueId()).isMuted())
        {
            event.getPlayer().sendMessage(PlexUtils.messageComponent("muted"));
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCommand(PlayerCommandPreprocessEvent event)
    {
        if (plugin.getPlayerCache().getPlexPlayer(event.getPlayer().getUniqueId()).isMuted())
        {
            String message = event.getMessage();
            // Don't check the arguments
            message = message.replaceAll("\\s.*", "").replaceFirst("/", "");
            PlexLog.debug("message: " + message);

            // Check regular command
            if (commands.contains(message.toLowerCase()))
            {
                PlexLog.debug("Matches command");
                event.getPlayer().sendMessage(PlexUtils.messageComponent("muted"));
                event.setCancelled(true);
                return;
            }

            for (String command : commands)
            {
                Command cmd = Bukkit.getCommandMap().getCommand(command);
                if (cmd == null)
                {
                    PlexLog.debug("Null command");
                    return;
                }
                if (cmd.getAliases().contains(message.toLowerCase()))
                {
                    PlexLog.debug("Matches alias");
                    event.getPlayer().sendMessage(PlexUtils.messageComponent("muted"));
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }
}
