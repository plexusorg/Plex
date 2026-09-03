package dev.plex.services.impl;

import dev.plex.Plex;
import dev.plex.util.PlexLog;
import dev.plex.util.UpdateChecker;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;

public class UpdateCheckerService
{
    private final Plex plugin;
    private volatile ScheduledTask task;

    public UpdateCheckerService(Plex plugin)
    {
        this.plugin = plugin;
    }

    public synchronized void start()
    {
        stop();
        if (!plugin.config.getBoolean("updater.enabled", true))
        {
            return;
        }
        plugin.getUpdateChecker().clearCache();
        task = Bukkit.getAsyncScheduler().runAtFixedRate(
                plugin, this::run, 1, repeatInSeconds(), TimeUnit.SECONDS);
    }

    public void run(ScheduledTask task)
    {
        if (this.task != task)
        {
            return;
        }
        try
        {
            UpdateChecker.UpdateCheckResult result = plugin.getUpdateChecker().checkForUpdates(false);
            if (this.task != task)
            {
                return;
            }
            if (result.status() == UpdateChecker.UpdateCheckStatus.UPDATE_AVAILABLE
                    || result.status() == UpdateChecker.UpdateCheckStatus.MINECRAFT_TOO_OLD
                    || result.status() == UpdateChecker.UpdateCheckStatus.MINECRAFT_TOO_NEW
                    || result.status() == UpdateChecker.UpdateCheckStatus.MINECRAFT_UNLISTED)
            {
                synchronized (this)
                {
                    if (this.task == task)
                    {
                        task.cancel();
                        this.task = null;
                    }
                }
            }
            plugin.getUpdateChecker().sendResultMessage(Bukkit.getConsoleSender(), result, 1);
        }
        catch (RuntimeException exception)
        {
            PlexLog.warn("Update check failed; retrying later: {0}", exception.getMessage());
        }
    }

    public synchronized void stop()
    {
        if (task != null)
        {
            task.cancel();
            task = null;
        }
    }

    public boolean isRunning()
    {
        return task != null;
    }

    private int repeatInSeconds()
    {
        return Math.max(60, plugin.config.getInt("updater.interval", 1800));
    }
}
