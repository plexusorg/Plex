package dev.plex.api.impl;

import dev.plex.Plex;
import dev.plex.api.scheduler.SchedulerApi;

final class DefaultSchedulerApi implements SchedulerApi
{
    private final Plex plugin;

    DefaultSchedulerApi(Plex plugin) { this.plugin = plugin; }

    @Override public Object runSync(Runnable task) { return plugin.getServer().getScheduler().runTask(plugin, task); }
    @Override public Object runLater(Runnable task, long delayTicks) { return plugin.getServer().getScheduler().runTaskLater(plugin, task, delayTicks); }
    @Override public Object runTimer(Runnable task, long delayTicks, long periodTicks) { return plugin.getServer().getScheduler().runTaskTimer(plugin, task, delayTicks, periodTicks); }
}
