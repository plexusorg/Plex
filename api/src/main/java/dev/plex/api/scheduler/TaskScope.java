package dev.plex.api.scheduler;

/**
 * Tracks a group of scheduled tasks.
 */
public interface TaskScope extends SchedulerApi, AutoCloseable
{
    /**
     * Cancels all tasks in this group.
     */
    void cancelAll();

    /**
     * Cancels all tasks in this group.
     */
    @Override
    default void close()
    {
        cancelAll();
    }
}
