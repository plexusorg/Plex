package dev.plex.command.source;

import lombok.Getter;
import dev.plex.cache.PlayerCache;
import dev.plex.player.PlexPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

@Getter
public class CommandSource
{
    private final CommandSender sender;
    private final Player player;
    private final PlexPlayer plexPlayer;
    private final boolean isConsoleSender;

    public CommandSource(CommandSender sender)
    {
        this.sender = sender;
        this.isConsoleSender = !(sender instanceof Player);
        this.player = sender instanceof Player ? Bukkit.getPlayer(sender.getName()) : null;
        this.plexPlayer = sender instanceof Player ? PlayerCache.getPlexPlayerMap().get(((Player)sender).getUniqueId()) : null;
    }

    // there's a bug here where it sends it to the player not the console
    // i assume this is because there's no checking. no idea why but it always sends it to the player even if executed from the console
    public void send(String s)
    {
        sender.sendMessage(s);
    }

    public String getName()
    {
        return sender.getName();
    }
}