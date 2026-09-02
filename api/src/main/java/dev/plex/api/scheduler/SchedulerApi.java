package dev.plex.api.scheduler;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

/**
 * Runs tasks on Paper and Folia schedulers.
 * Adventure audience messaging does not require a scheduler task.
 */
public interface SchedulerApi
{
    /**
     * Creates a task group that can cancel all of its tasks.
     *
     * @return new task group
     */
    TaskScope taskScope();

    /**
     * Returns an executor that uses Paper's asynchronous scheduler.
     * Use it for blocking I/O and CPU work that does not use Bukkit world,
     * entity, inventory, or command state.
     *
     * @return async scheduler executor
     */
    Executor asyncExecutor();

    /**
     * Runs work on the global region.
     * Use it for console commands, world time, weather, game rules, and plugin state.
     *
     * @param task task to execute
     */
    void executeGlobal(Runnable task);

    /**
     * Runs work on the next global-region tick.
     *
     * @param task task consumer receiving the scheduled task
     * @return scheduled task handle
     * @see #executeGlobal(Runnable)
     */
    ScheduledTask runGlobal(Consumer<ScheduledTask> task);

    /**
     * Runs work on the next global-region tick.
     *
     * @param task task to run
     * @return scheduled task handle
     * @see #runGlobal(Consumer)
     */
    default ScheduledTask runGlobal(Runnable task)
    {
        return runGlobal(scheduledTask -> task.run());
    }

    /**
     * Runs work on the global region after a tick delay.
     *
     * @param task task consumer receiving the scheduled task
     * @param delayTicks delay in server ticks
     * @return scheduled task handle
     * @see #executeGlobal(Runnable)
     */
    ScheduledTask runGlobalLater(Consumer<ScheduledTask> task, long delayTicks);

    /**
     * Runs work on the global region after a tick delay.
     *
     * @param task task to run
     * @param delayTicks delay in server ticks
     * @return scheduled task handle
     * @see #runGlobalLater(Consumer, long)
     */
    default ScheduledTask runGlobalLater(Runnable task, long delayTicks)
    {
        return runGlobalLater(scheduledTask -> task.run(), delayTicks);
    }

    /**
     * Runs repeating work on the global region.
     *
     * @param task task consumer receiving the scheduled task
     * @param delayTicks initial delay in server ticks
     * @param periodTicks repeat interval in server ticks
     * @return scheduled task handle
     * @see #executeGlobal(Runnable)
     */
    ScheduledTask runGlobalTimer(Consumer<ScheduledTask> task, long delayTicks, long periodTicks);

    /**
     * Runs repeating work on the global region.
     *
     * @param task task to run
     * @param delayTicks initial delay in server ticks
     * @param periodTicks repeat interval in server ticks
     * @return scheduled task handle
     * @see #runGlobalTimer(Consumer, long, long)
     */
    default ScheduledTask runGlobalTimer(Runnable task, long delayTicks, long periodTicks)
    {
        return runGlobalTimer(scheduledTask -> task.run(), delayTicks, periodTicks);
    }

    /**
     * Runs work on Paper's async scheduler.
     * Do not touch Bukkit world, entity, inventory, or command state here.
     *
     * @param task task consumer receiving the scheduled task
     * @return scheduled task handle
     */
    ScheduledTask runAsync(Consumer<ScheduledTask> task);

    /**
     * Runs work on Paper's async scheduler.
     *
     * @param task task to run
     * @return scheduled task handle
     * @see #runAsync(Consumer)
     */
    default ScheduledTask runAsync(Runnable task)
    {
        return runAsync(scheduledTask -> task.run());
    }

    /**
     * Runs asynchronous work after a time delay.
     *
     * @param task task consumer receiving the scheduled task
     * @param delay delay amount
     * @param unit delay time unit
     * @return scheduled task handle
     * @see #runAsync(Consumer)
     */
    ScheduledTask runAsyncLater(Consumer<ScheduledTask> task, long delay, TimeUnit unit);

    /**
     * Runs asynchronous work after a time delay.
     *
     * @param task task to run
     * @param delay delay amount
     * @param unit delay time unit
     * @return scheduled task handle
     * @see #runAsyncLater(Consumer, long, TimeUnit)
     */
    default ScheduledTask runAsyncLater(Runnable task, long delay, TimeUnit unit)
    {
        return runAsyncLater(scheduledTask -> task.run(), delay, unit);
    }

    /**
     * Runs repeating asynchronous work at a time interval.
     *
     * @param task task consumer receiving the scheduled task
     * @param delay initial delay amount
     * @param period repeat interval amount
     * @param unit delay and period time unit
     * @return scheduled task handle
     * @see #runAsync(Consumer)
     */
    ScheduledTask runAsyncTimer(Consumer<ScheduledTask> task, long delay, long period, TimeUnit unit);

    /**
     * Runs repeating asynchronous work at a time interval.
     *
     * @param task task to run
     * @param delay initial delay amount
     * @param period repeat interval amount
     * @param unit delay and period time unit
     * @return scheduled task handle
     * @see #runAsyncTimer(Consumer, long, long, TimeUnit)
     */
    default ScheduledTask runAsyncTimer(Runnable task, long delay, long period, TimeUnit unit)
    {
        return runAsyncTimer(scheduledTask -> task.run(), delay, period, unit);
    }

    /**
     * Runs work on the region that owns the location.
     * Use this for block, chunk, and location-bound world access.
     *
     * @param location location for the task region
     * @param task task to execute
     */
    void executeRegion(Location location, Runnable task);

    /**
     * Runs work on the region that owns the chunk.
     * Use this for block, chunk, and location-bound world access.
     *
     * @param world world containing the chunk
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @param task task to execute
     */
    void executeRegion(World world, int chunkX, int chunkZ, Runnable task);

    /**
     * Runs work on the next tick of the location's region.
     *
     * @param location location for the task region
     * @param task task consumer receiving the scheduled task
     * @return scheduled task handle
     * @see #executeRegion(Location, Runnable)
     */
    ScheduledTask runRegion(Location location, Consumer<ScheduledTask> task);

    /**
     * Runs work on the next tick of the location's region.
     *
     * @param location location for the task region
     * @param task task to run
     * @return scheduled task handle
     * @see #runRegion(Location, Consumer)
     */
    default ScheduledTask runRegion(Location location, Runnable task)
    {
        return runRegion(location, scheduledTask -> task.run());
    }

    /**
     * Runs work on the next tick of the chunk's region.
     *
     * @param world world containing the chunk
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @param task task consumer receiving the scheduled task
     * @return scheduled task handle
     * @see #executeRegion(World, int, int, Runnable)
     */
    ScheduledTask runRegion(World world, int chunkX, int chunkZ, Consumer<ScheduledTask> task);

    /**
     * Runs work on the next tick of the chunk's region.
     *
     * @param world world containing the chunk
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @param task task to run
     * @return scheduled task handle
     * @see #runRegion(World, int, int, Consumer)
     */
    default ScheduledTask runRegion(World world, int chunkX, int chunkZ, Runnable task)
    {
        return runRegion(world, chunkX, chunkZ, scheduledTask -> task.run());
    }

    /**
     * Runs work on a location's owning region after a tick delay.
     *
     * @param location location for the task region
     * @param task task consumer receiving the scheduled task
     * @param delayTicks delay in server ticks
     * @return scheduled task handle
     * @see #executeRegion(Location, Runnable)
     */
    ScheduledTask runRegionLater(Location location, Consumer<ScheduledTask> task, long delayTicks);

    /**
     * Runs work on a location's owning region after a tick delay.
     *
     * @param location location for the task region
     * @param task task to run
     * @param delayTicks delay in server ticks
     * @return scheduled task handle
     * @see #runRegionLater(Location, Consumer, long)
     */
    default ScheduledTask runRegionLater(Location location, Runnable task, long delayTicks)
    {
        return runRegionLater(location, scheduledTask -> task.run(), delayTicks);
    }

    /**
     * Runs work on a chunk's owning region after a tick delay.
     *
     * @param world world containing the chunk
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @param task task consumer receiving the scheduled task
     * @param delayTicks delay in server ticks
     * @return scheduled task handle
     * @see #executeRegion(World, int, int, Runnable)
     */
    ScheduledTask runRegionLater(World world, int chunkX, int chunkZ, Consumer<ScheduledTask> task, long delayTicks);

    /**
     * Runs work on a chunk's owning region after a tick delay.
     *
     * @param world world containing the chunk
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @param task task to run
     * @param delayTicks delay in server ticks
     * @return scheduled task handle
     * @see #runRegionLater(World, int, int, Consumer, long)
     */
    default ScheduledTask runRegionLater(World world, int chunkX, int chunkZ, Runnable task, long delayTicks)
    {
        return runRegionLater(world, chunkX, chunkZ, scheduledTask -> task.run(), delayTicks);
    }

    /**
     * Runs repeating work on a location's owning region.
     *
     * @param location location whose owning region should run the task
     * @param task task consumer receiving the scheduled task
     * @param delayTicks initial delay in server ticks
     * @param periodTicks repeat interval in server ticks
     * @return scheduled task handle
     * @see #executeRegion(Location, Runnable)
     */
    ScheduledTask runRegionTimer(Location location, Consumer<ScheduledTask> task, long delayTicks, long periodTicks);

    /**
     * Runs repeating work on a location's owning region.
     *
     * @param location location whose owning region should run the task
     * @param task task to run
     * @param delayTicks initial delay in server ticks
     * @param periodTicks repeat interval in server ticks
     * @return scheduled task handle
     * @see #runRegionTimer(Location, Consumer, long, long)
     */
    default ScheduledTask runRegionTimer(Location location, Runnable task, long delayTicks, long periodTicks)
    {
        return runRegionTimer(location, scheduledTask -> task.run(), delayTicks, periodTicks);
    }

    /**
     * Runs repeating work on a chunk's owning region.
     *
     * @param world world containing the chunk
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @param task task consumer receiving the scheduled task
     * @param delayTicks initial delay in server ticks
     * @param periodTicks repeat interval in server ticks
     * @return scheduled task handle
     * @see #executeRegion(World, int, int, Runnable)
     */
    ScheduledTask runRegionTimer(World world, int chunkX, int chunkZ, Consumer<ScheduledTask> task, long delayTicks, long periodTicks);

    /**
     * Runs repeating work on a chunk's owning region.
     *
     * @param world world containing the chunk
     * @param chunkX chunk X coordinate
     * @param chunkZ chunk Z coordinate
     * @param task task to run
     * @param delayTicks initial delay in server ticks
     * @param periodTicks repeat interval in server ticks
     * @return scheduled task handle
     * @see #runRegionTimer(World, int, int, Consumer, long, long)
     */
    default ScheduledTask runRegionTimer(World world, int chunkX, int chunkZ, Runnable task, long delayTicks, long periodTicks)
    {
        return runRegionTimer(world, chunkX, chunkZ, scheduledTask -> task.run(), delayTicks, periodTicks);
    }

    /**
     * Runs work on the region that owns the entity.
     * Use this for player and entity state access, inventory changes, kicks,
     * teleports, passengers, potion effects, and other entity work.
     * Paper runs the retired action if it removes the entity before the task runs.
     *
     * @param entity entity whose scheduler should run the task
     * @param task task to execute
     * @param retired action to run if Paper removes the entity first
     * @param delayTicks delay in server ticks
     * @return true if Paper accepted the task
     */
    boolean executeEntity(Entity entity, Runnable task, @Nullable Runnable retired, long delayTicks);

    /**
     * Runs work on the region that owns the entity.
     *
     * @param entity entity whose scheduler should run the task
     * @param task task to execute
     * @param delayTicks delay in server ticks
     * @return {@code true} if Paper accepted the task
     * @see #executeEntity(Entity, Runnable, Runnable, long)
     */
    default boolean executeEntity(Entity entity, Runnable task, long delayTicks)
    {
        return executeEntity(entity, task, null, delayTicks);
    }

    /**
     * Runs work on the next tick of the entity's owning region.
     *
     * @param entity entity whose scheduler should run the task
     * @param task task consumer receiving the scheduled task
     * @param retired action to run if Paper removes the entity first
     * @return scheduled task handle, or {@code null} if the entity is retired
     * @see #executeEntity(Entity, Runnable, Runnable, long)
     */
    @Nullable
    ScheduledTask runEntity(Entity entity, Consumer<ScheduledTask> task, @Nullable Runnable retired);

    /**
     * Runs work on the next tick of the entity's owning region.
     *
     * @param entity entity whose scheduler should run the task
     * @param task task to run
     * @return scheduled task handle, or {@code null} if the entity is retired
     * @see #runEntity(Entity, Consumer, Runnable)
     */
    default @Nullable ScheduledTask runEntity(Entity entity, Runnable task)
    {
        return runEntity(entity, scheduledTask -> task.run(), null);
    }

    /**
     * Runs work on the entity's owning region after a tick delay.
     *
     * @param entity entity whose scheduler should run the task
     * @param task task consumer receiving the scheduled task
     * @param retired action to run if Paper removes the entity first
     * @param delayTicks delay in server ticks
     * @return scheduled task handle, or {@code null} if the entity is retired
     * @see #executeEntity(Entity, Runnable, Runnable, long)
     */
    @Nullable
    ScheduledTask runEntityLater(Entity entity, Consumer<ScheduledTask> task, @Nullable Runnable retired, long delayTicks);

    /**
     * Runs work on the entity's owning region after a tick delay.
     *
     * @param entity entity whose scheduler should run the task
     * @param task task to run
     * @param delayTicks delay in server ticks
     * @return scheduled task handle, or {@code null} if the entity is retired
     * @see #runEntityLater(Entity, Consumer, Runnable, long)
     */
    default @Nullable ScheduledTask runEntityLater(Entity entity, Runnable task, long delayTicks)
    {
        return runEntityLater(entity, scheduledTask -> task.run(), null, delayTicks);
    }

    /**
     * Runs repeating work on the entity's owning region.
     *
     * @param entity entity whose scheduler should run the task
     * @param task task consumer receiving the scheduled task
     * @param retired action to run if Paper removes the entity first
     * @param delayTicks initial delay in server ticks
     * @param periodTicks repeat interval in server ticks
     * @return scheduled task handle, or {@code null} if the entity is retired
     * @see #executeEntity(Entity, Runnable, Runnable, long)
     */
    @Nullable
    ScheduledTask runEntityTimer(Entity entity, Consumer<ScheduledTask> task, @Nullable Runnable retired, long delayTicks, long periodTicks);

    /**
     * Runs repeating work on the entity's owning region.
     *
     * @param entity entity whose scheduler should run the task
     * @param task task to run
     * @param delayTicks initial delay in server ticks
     * @param periodTicks repeat interval in server ticks
     * @return scheduled task handle, or {@code null} if the entity is retired
     * @see #runEntityTimer(Entity, Consumer, Runnable, long, long)
     */
    default @Nullable ScheduledTask runEntityTimer(Entity entity, Runnable task, long delayTicks, long periodTicks)
    {
        return runEntityTimer(entity, scheduledTask -> task.run(), null, delayTicks, periodTicks);
    }

}
