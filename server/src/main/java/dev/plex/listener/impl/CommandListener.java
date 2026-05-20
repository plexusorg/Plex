package dev.plex.listener.impl;

import dev.plex.Plex;
import dev.plex.listener.ServerListenerBase;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexUtils;
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
        Bukkit.getOnlinePlayers().stream().filter(pl ->
        {
            PlexPlayer player = plugin.getPlayerCache().getPlexPlayer(pl.getUniqueId());
            return player.isCommandSpy() && hasCommandSpy(plugin.getPlayerCache().getPlexPlayer(pl.getUniqueId()));
        }).forEach(pl ->
        {
            Player player = event.getPlayer();
            String command = event.getMessage();
            if (!pl.getUniqueId().equals(player.getUniqueId()))
            {
                pl.sendMessage(PlexUtils.messageComponent("commandSpyFormat", player.getName(), command));
            }
        });
    }

    private boolean hasCommandSpy(PlexPlayer plexPlayer)
    {
        return plexPlayer.getPlayer().hasPermission("plex.commandspy");
    }
}
