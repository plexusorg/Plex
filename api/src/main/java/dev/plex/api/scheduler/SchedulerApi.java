package dev.plex.api.scheduler;

public interface SchedulerApi
{
    Object runSync(Runnable task);
    Object runLater(Runnable task, long delayTicks);
    Object runTimer(Runnable task, long delayTicks, long periodTicks);
}
