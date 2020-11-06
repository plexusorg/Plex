package me.totalfreedom.plex.command;

import java.util.List;
import me.totalfreedom.plex.command.source.CommandSource;
import org.bukkit.command.CommandSender;

public interface IPlexCommand
{
    void execute(CommandSource sender, String[] args);

    List<String> onTabComplete(CommandSource sender, String[] args);
}
