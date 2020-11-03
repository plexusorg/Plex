package me.totalfreedom.plex.command.source;

import lombok.Getter;
import me.totalfreedom.plex.cache.PlayerCache;
import me.totalfreedom.plex.player.PlexPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Getter
public class CommandSource
{
    private CommandSender sender;
    private final Player player;
    private final PlexPlayer plexPlayer;

    public CommandSource(CommandSender sender)
    {
        this.sender = sender;
        this.player = sender instanceof Player ? Bukkit.getPlayer(sender.getName()) : null;
        this.plexPlayer = sender instanceof Player ? PlayerCache.getPlexPlayerMap().get(((Player) sender).getUniqueId()) : null;
    }

    public void send(String s)
    {
        sender.sendMessage(s);
    }

    public String getName()
    {
        return sender.getName();
    }
}