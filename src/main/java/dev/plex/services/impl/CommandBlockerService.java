package dev.plex.services.impl;

import dev.plex.services.AbstractService;
import dev.plex.util.PlexLog;

public class CommandBlockerService extends AbstractService
{
    public CommandBlockerService()
    {
        super(false, true);
    }

    @Override
    public void run()
    {
        plugin.getCommandBlockerManager().syncCommands();
        PlexLog.log("Command Blocker commands loaded");
    }

    @Override
    public int repeatInSeconds()
    {
        return 0;
    }
}