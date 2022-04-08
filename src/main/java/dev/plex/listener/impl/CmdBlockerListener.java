package dev.plex.listener.impl;

import dev.plex.cache.DataUtils;
import dev.plex.cmdblocker.BaseCommand;
import dev.plex.cmdblocker.MatchCommand;
import dev.plex.cmdblocker.RegexCommand;
import dev.plex.listener.PlexListener;
import dev.plex.player.PlexPlayer;
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
        if (!plugin.getCommandBlockerManager().loadedYet)
        {
            event.setCancelled(true);
            return;
        }
        Player player = event.getPlayer();
        String message = event.getMessage().substring(1);
        for (BaseCommand blockedCommand : plugin.getCommandBlockerManager().getBlockedCommands()) {
            if (DataUtils.getPlayer(player.getUniqueId()).getRankFromString().ordinal() > blockedCommand.getRank().ordinal())
            {
                return;
            }
            boolean isBlocked = false;
            if (blockedCommand instanceof RegexCommand regexCommand)
            {
                if (regexCommand.getRegex().matcher(message).matches())
                {
                    isBlocked = true;
                }
            }
            else if(blockedCommand instanceof MatchCommand matchCommand)
            {
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
