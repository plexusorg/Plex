package dev.plex.command;

import dev.plex.command.source.CommandSource;

import java.util.List;

public interface IPlexCommand
{
    void execute(CommandSource sender, String[] args);

    List<String> onTabComplete(CommandSource sender, String[] args);
}
