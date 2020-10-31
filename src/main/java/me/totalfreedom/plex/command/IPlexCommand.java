package me.totalfreedom.plex.command;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface IPlexCommand
{

    void execute(CommandSender sender, String[] args);
    List<String> onTabComplete(CommandSender sender, String[] args);

}
