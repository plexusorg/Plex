package dev.plex.services.impl;

import dev.plex.Plex;
import dev.plex.util.PlexLog;
import dev.plex.util.UpdateChecker;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.Bukkit;

public class UpdateCheckerService
{
    private final Plex plugin;
    private boolean notified;

    public UpdateCheckerService(Plex plugin)
    {
        this.plugin = plugin;
    }

    public boolean shouldStart()
    {
        return plugin.config.getBoolean("updater.enabled", true);
    }

    public void run(ScheduledTask task)
    {
        if (notified)
        {
            return;
        }
        try
        {
            UpdateChecker.UpdateCheckResult result = plugin.getUpdateChecker().checkForUpdates(false);
            if (result.status() == UpdateChecker.UpdateCheckStatus.UPDATE_AVAILABLE
                    || result.status() == UpdateChecker.UpdateCheckStatus.MINECRAFT_TOO_OLD
                    || result.status() == UpdateChecker.UpdateCheckStatus.MINECRAFT_TOO_NEW
                    || result.status() == UpdateChecker.UpdateCheckStatus.MINECRAFT_UNLISTED)
            {
                notified = true;
                task.cancel();
            }
            plugin.getUpdateChecker().sendResultMessage(Bukkit.getConsoleSender(), result, 1);
        }
        catch (RuntimeException exception)
        {
            PlexLog.warn("Update check failed; retrying later: {0}", exception.getMessage());
        }
    }

    public void onStart()
    {
        notified = false;
        plugin.getUpdateChecker().clearCache();
    }

    public int repeatInSeconds()
    {
        return Math.max(60, plugin.config.getInt("updater.interval", 1800));
    }
}
