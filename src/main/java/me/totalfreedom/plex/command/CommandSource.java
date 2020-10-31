package me.totalfreedom.plex.command;

import lombok.Getter;
import me.totalfreedom.plex.player.PlexPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandSource
{
    @Getter
    private CommandSender sender;
    @Getter
    private Player player;
    private PlexPlayer plexPlayer;

    public CommandSource(CommandSender sender)
    {
        this.sender = sender;
        this.player = Bukkit.getPlayer(sender.getName());
        this.plexPlayer = null;
    }


}