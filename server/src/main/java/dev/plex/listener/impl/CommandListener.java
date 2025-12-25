package dev.plex.listener.impl;

import dev.plex.listener.PlexListener;
import dev.plex.player.PlexPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

public class CommandListener extends PlexListener
{
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
                pl.sendMessage(Component.text(player.getName() + ": " + command).color(NamedTextColor.GRAY));
            }
        });
    }

    private boolean hasCommandSpy(PlexPlayer plexPlayer)
    {
        return plexPlayer.getPlayer().hasPermission("plex.commandspy");
    }
}
