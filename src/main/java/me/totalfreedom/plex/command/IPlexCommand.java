package me.totalfreedom.plex.command;

import me.totalfreedom.plex.command.source.CommandSource;

import java.util.List;

public interface IPlexCommand
{

    void execute(CommandSource sender, String[] args);
    List<String> onTabComplete(CommandSource sender, String[] args);

}
