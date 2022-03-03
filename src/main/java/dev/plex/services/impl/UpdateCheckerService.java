package dev.plex.services.impl;

import dev.plex.services.AbstractService;

public class UpdateCheckerService extends AbstractService
{
    public UpdateCheckerService()
    {
        super(true, true);
    }

    private boolean newVersion = false;

    @Override
    public void run()
    {
        if (!newVersion)
        {
            if (plugin.getUpdateChecker().check())
            {
                newVersion = true;
            }
        }
    }

    @Override
    public int repeatInSeconds()
    {
        // Every 30 minutes
        return 1800;
    }
}
