package me.totalfreedom.plex.command;

import me.totalfreedom.plex.PlexBase;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class PlexCommand extends PlexBase implements CommandExecutor
{
    @Override
    public boolean onCommand(CommandSender sender, Command message, String s, String[] args)
    {
        sender.sendMessage(plugin.config.getString("server.test"));
        return true;
    }
}
