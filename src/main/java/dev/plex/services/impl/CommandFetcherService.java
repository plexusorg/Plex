package dev.plex.services.impl;

import dev.plex.services.AbstractService;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;

public class CommandFetcherService extends AbstractService
{
    public static final List<Command> worldeditCommands = new ArrayList<>();

    public CommandFetcherService()
    {
        super(false, false);
    }

    @Override
    public void run()
    {
    }

    @Override
    public int repeatInSeconds()
    {
        return 0;
    }
}
