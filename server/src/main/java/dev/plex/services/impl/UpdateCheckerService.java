package dev.plex.services.impl;

import dev.plex.Plex;
import dev.plex.services.AbstractService;
import dev.plex.util.UpdateChecker;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;

public class UpdateCheckerService extends AbstractService
{
    private boolean notified = false;

    public UpdateCheckerService(Plex plugin)
    {
        super(plugin, true, true);
    }

    @Override
    public void run(ScheduledTask task)
    {
        if (!notified)
        {
            UpdateChecker.UpdateCheckResult result = plugin.getUpdateChecker().checkForUpdates(false);
            plugin.getUpdateChecker().sendResultMessage(Bukkit.getConsoleSender(), result, 1);
            if (result.status() == UpdateChecker.UpdateCheckStatus.UPDATE_AVAILABLE
                    || result.status() == UpdateChecker.UpdateCheckStatus.MINECRAFT_TOO_OLD
                    || result.status() == UpdateChecker.UpdateCheckStatus.MINECRAFT_TOO_NEW
                    || result.status() == UpdateChecker.UpdateCheckStatus.MINECRAFT_UNLISTED)
            {
                notified = true;
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
