package dev.plex.services.impl;

import dev.plex.services.AbstractService;

public class UpdateCheckerService extends AbstractService
{
    private boolean newVersion = false;

    public UpdateCheckerService()
    {
        super(true, true);
    }

    @Override
    public void run()
    {
        if (!newVersion)
        {
            if (plugin.getUpdateChecker().getUpdateStatusMessage("plexusorg/Plex", "master"))
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
