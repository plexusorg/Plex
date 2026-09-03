package dev.plex.services.impl;

import org.bukkit.Bukkit;

import dev.plex.Plex;
import dev.plex.util.PlexLog;
import dev.plex.util.UpdateChecker;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class UpdateCheckerService
{
    private final Plex plugin;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(
            Thread.ofPlatform().daemon().name("Plex-Update-Checker").factory());
    private volatile ScheduledFuture<?> task;
    private int generation;

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
        int activeGeneration = ++generation;
        task = executor.scheduleWithFixedDelay(
                () -> check(activeGeneration), 1, repeatInSeconds(), TimeUnit.SECONDS);
    }

    private void check(int activeGeneration)
    {
        if (generation != activeGeneration)
        {
            return;
        }
        try
        {
            UpdateChecker.UpdateCheckResult result = plugin.getUpdateChecker().checkForUpdates(false);
            if (generation != activeGeneration)
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
                    if (generation == activeGeneration && task != null)
                    {
                        task.cancel(false);
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
            task.cancel(false);
            task = null;
        }
        generation++;
    }

    public synchronized void close()
    {
        stop();
        executor.shutdownNow();
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
