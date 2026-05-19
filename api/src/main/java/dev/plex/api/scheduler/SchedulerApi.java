package dev.plex.api.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

public interface SchedulerApi
{
    Executor asyncExecutor();

    void executeGlobal(Runnable task);

    ScheduledTask runGlobal(Consumer<ScheduledTask> task);

    default ScheduledTask runGlobal(Runnable task)
    {
        return runGlobal(scheduledTask -> task.run());
    }

    ScheduledTask runGlobalLater(Consumer<ScheduledTask> task, long delayTicks);

    default ScheduledTask runGlobalLater(Runnable task, long delayTicks)
    {
        return runGlobalLater(scheduledTask -> task.run(), delayTicks);
    }

    ScheduledTask runGlobalTimer(Consumer<ScheduledTask> task, long delayTicks, long periodTicks);

    default ScheduledTask runGlobalTimer(Runnable task, long delayTicks, long periodTicks)
    {
        return runGlobalTimer(scheduledTask -> task.run(), delayTicks, periodTicks);
    }

    ScheduledTask runAsync(Consumer<ScheduledTask> task);

    default ScheduledTask runAsync(Runnable task)
    {
        return runAsync(scheduledTask -> task.run());
    }

    ScheduledTask runAsyncLater(Consumer<ScheduledTask> task, long delay, TimeUnit unit);

    default ScheduledTask runAsyncLater(Runnable task, long delay, TimeUnit unit)
    {
        return runAsyncLater(scheduledTask -> task.run(), delay, unit);
    }

    ScheduledTask runAsyncTimer(Consumer<ScheduledTask> task, long delay, long period, TimeUnit unit);

    default ScheduledTask runAsyncTimer(Runnable task, long delay, long period, TimeUnit unit)
    {
        return runAsyncTimer(scheduledTask -> task.run(), delay, period, unit);
    }

    void executeRegion(Location location, Runnable task);

    void executeRegion(World world, int chunkX, int chunkZ, Runnable task);

    ScheduledTask runRegion(Location location, Consumer<ScheduledTask> task);

    default ScheduledTask runRegion(Location location, Runnable task)
    {
        return runRegion(location, scheduledTask -> task.run());
    }

    ScheduledTask runRegion(World world, int chunkX, int chunkZ, Consumer<ScheduledTask> task);

    default ScheduledTask runRegion(World world, int chunkX, int chunkZ, Runnable task)
    {
        return runRegion(world, chunkX, chunkZ, scheduledTask -> task.run());
    }

    ScheduledTask runRegionLater(Location location, Consumer<ScheduledTask> task, long delayTicks);

    default ScheduledTask runRegionLater(Location location, Runnable task, long delayTicks)
    {
        return runRegionLater(location, scheduledTask -> task.run(), delayTicks);
    }

    ScheduledTask runRegionLater(World world, int chunkX, int chunkZ, Consumer<ScheduledTask> task, long delayTicks);

    default ScheduledTask runRegionLater(World world, int chunkX, int chunkZ, Runnable task, long delayTicks)
    {
        return runRegionLater(world, chunkX, chunkZ, scheduledTask -> task.run(), delayTicks);
    }

    ScheduledTask runRegionTimer(Location location, Consumer<ScheduledTask> task, long delayTicks, long periodTicks);

    default ScheduledTask runRegionTimer(Location location, Runnable task, long delayTicks, long periodTicks)
    {
        return runRegionTimer(location, scheduledTask -> task.run(), delayTicks, periodTicks);
    }

    ScheduledTask runRegionTimer(World world, int chunkX, int chunkZ, Consumer<ScheduledTask> task, long delayTicks, long periodTicks);

    default ScheduledTask runRegionTimer(World world, int chunkX, int chunkZ, Runnable task, long delayTicks, long periodTicks)
    {
        return runRegionTimer(world, chunkX, chunkZ, scheduledTask -> task.run(), delayTicks, periodTicks);
    }

    boolean executeEntity(Entity entity, Runnable task, @Nullable Runnable retired, long delayTicks);

    default boolean executeEntity(Entity entity, Runnable task, long delayTicks)
    {
        return executeEntity(entity, task, null, delayTicks);
    }

    @Nullable
    ScheduledTask runEntity(Entity entity, Consumer<ScheduledTask> task, @Nullable Runnable retired);

    default @Nullable ScheduledTask runEntity(Entity entity, Runnable task)
    {
        return runEntity(entity, scheduledTask -> task.run(), null);
    }

    @Nullable
    ScheduledTask runEntityLater(Entity entity, Consumer<ScheduledTask> task, @Nullable Runnable retired, long delayTicks);

    default @Nullable ScheduledTask runEntityLater(Entity entity, Runnable task, long delayTicks)
    {
        return runEntityLater(entity, scheduledTask -> task.run(), null, delayTicks);
    }

    @Nullable
    ScheduledTask runEntityTimer(Entity entity, Consumer<ScheduledTask> task, @Nullable Runnable retired, long delayTicks, long periodTicks);

    default @Nullable ScheduledTask runEntityTimer(Entity entity, Runnable task, long delayTicks, long periodTicks)
    {
        return runEntityTimer(entity, scheduledTask -> task.run(), null, delayTicks, periodTicks);
    }

    void cancelGlobalTasks();

    void cancelAsyncTasks();
}
