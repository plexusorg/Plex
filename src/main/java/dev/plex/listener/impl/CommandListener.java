package dev.plex.listener.impl;

import dev.plex.cache.DataUtils;
import dev.plex.cache.player.PlayerCache;
import dev.plex.cmdblocker.BaseCommand;
import dev.plex.cmdblocker.MatchCommand;
import dev.plex.cmdblocker.RegexCommand;
import dev.plex.listener.PlexListener;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandListener extends PlexListener
{
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event)
    {
        Bukkit.getOnlinePlayers().stream().filter(pl -> PlayerCache.getPlexPlayer(pl.getUniqueId()).isCommandSpy()).forEach(pl ->
        {
            Player player = event.getPlayer();
            String command = event.getMessage();
            if (pl != player)
            {
                pl.sendMessage(ChatColor.GRAY + player.getName() + ": " + command);
            }
        });

        if (!plugin.getCommandBlockerManager().loadedYet)
        {
            event.setCancelled(true);
            return;
        }
        Player player = event.getPlayer();
        String message = event.getMessage().substring(1).stripLeading(); // stripLeading() is VITAL for workaround blocking (/ minecraft:summon)
        for (BaseCommand blockedCommand : plugin.getCommandBlockerManager().getBlockedCommands())
        {
            PlexPlayer plexPlayer = DataUtils.getPlayer(player.getUniqueId());
            if (!plexPlayer.getRankFromString().isAtMost(blockedCommand.getRank()))
            {
                continue;
            }

            boolean isBlocked = false;
            if (blockedCommand instanceof RegexCommand regexCommand)
            {
                if (regexCommand.getRegex().matcher(message).lookingAt())
                {
                    isBlocked = true;
                }
            }
            else if (blockedCommand instanceof MatchCommand matchCommand)
            {
                if (message.toLowerCase().startsWith(matchCommand.getMatch().toLowerCase()))
                {
                    isBlocked = true;
                }
            }
            if (isBlocked)
            {
                event.setCancelled(true);
                player.sendMessage(MiniMessage.miniMessage().deserialize(PlexUtils.messageString("blockedCommandColor") + blockedCommand.getMessage()));
                return;
            }
        }
    }
}
