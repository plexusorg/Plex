package dev.plex.listener.impl;

import dev.plex.cmdblocker.BaseCommand;
import dev.plex.cmdblocker.MatchCommand;
import dev.plex.cmdblocker.RegexCommand;
import dev.plex.listener.PlexListener;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CmdBlockerListener extends PlexListener
{
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event)
    {
        Player player = event.getPlayer();
        String message = event.getMessage().substring(1);
        // check if commands are blocked here
        // DEFAULT message is named "commandBlocked"
        for (BaseCommand blockedCommand : plugin.getCommandBlockerManager().getBlockedCommands()) {
            //first check rank for if it applies
            //then check if command is blocked
            boolean isBlocked = false;
            if (blockedCommand instanceof RegexCommand regexCommand)
            {
                // regex
                if (regexCommand.getRegex().matcher(message).matches())
                {
                    isBlocked = true;
                }
            }
            else if(blockedCommand instanceof MatchCommand matchCommand)
            {
                // match command
                if (message.equalsIgnoreCase(matchCommand.getMessage()) || message.toLowerCase().startsWith(matchCommand.getMessage().toLowerCase() + " "))
                {
                    isBlocked = true;
                }
            }
            if (isBlocked)
            {
                event.setCancelled(true);
                player.sendMessage(MiniMessage.miniMessage().deserialize(PlexUtils.messageString("blockedCommandColor") + blockedCommand.getMessage()));
            }
        }
    }
}
