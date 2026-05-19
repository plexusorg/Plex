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
    /**
     * Executor backed by Paper's async scheduler.
     * Use it for blocking I/O and CPU work that does not touch Bukkit world,
     * entity, inventory, or command state.
     */
    Executor asyncExecutor();

    /**
     * Executes work on the global region.
     * Use the global region for state owned by Folia's global region, such as
     * console command dispatch, world time, weather, game rules, and plugin-level
     * coordination work.
     */
    void executeGlobal(Runnable task);

    /**
     * Runs work on the next global-region tick.
     *
     * @see #executeGlobal(Runnable)
     */
    ScheduledTask runGlobal(Consumer<ScheduledTask> task);

    default ScheduledTask runGlobal(Runnable task)
    {
        return runGlobal(scheduledTask -> task.run());
    }

    /**
     * Runs work on the global region after a tick delay.
     *
     * @see #executeGlobal(Runnable)
     */
    ScheduledTask runGlobalLater(Consumer<ScheduledTask> task, long delayTicks);

    default ScheduledTask runGlobalLater(Runnable task, long delayTicks)
    {
        return runGlobalLater(scheduledTask -> task.run(), delayTicks);
    }

    /**
     * Runs repeating work on the global region.
     *
     * @see #executeGlobal(Runnable)
     */
    ScheduledTask runGlobalTimer(Consumer<ScheduledTask> task, long delayTicks, long periodTicks);

    default ScheduledTask runGlobalTimer(Runnable task, long delayTicks, long periodTicks)
    {
        return runGlobalTimer(scheduledTask -> task.run(), delayTicks, periodTicks);
    }

    /**
     * Runs work on Paper's async scheduler.
     * Do not touch Bukkit world, entity, inventory, or command state here.
     */
    ScheduledTask runAsync(Consumer<ScheduledTask> task);

    default ScheduledTask runAsync(Runnable task)
    {
        return runAsync(scheduledTask -> task.run());
    }

    /**
     * Runs async work after a wall-clock delay.
     *
     * @see #runAsync(Consumer)
     */
    ScheduledTask runAsyncLater(Consumer<ScheduledTask> task, long delay, TimeUnit unit);

    default ScheduledTask runAsyncLater(Runnable task, long delay, TimeUnit unit)
    {
        return runAsyncLater(scheduledTask -> task.run(), delay, unit);
    }

    /**
     * Runs repeating async work on a wall-clock interval.
     *
     * @see #runAsync(Consumer)
     */
    ScheduledTask runAsyncTimer(Consumer<ScheduledTask> task, long delay, long period, TimeUnit unit);

    default ScheduledTask runAsyncTimer(Runnable task, long delay, long period, TimeUnit unit)
    {
        return runAsyncTimer(scheduledTask -> task.run(), delay, period, unit);
    }

    /**
     * Executes work on the region that owns the supplied location.
     * Use this for block, chunk, and location-bound world access.
     */
    void executeRegion(Location location, Runnable task);

    /**
     * Executes work on the region that owns the supplied chunk.
     * Use this for block, chunk, and location-bound world access.
     */
    void executeRegion(World world, int chunkX, int chunkZ, Runnable task);

    /**
     * Runs work on the next tick of the region that owns the supplied location.
     *
     * @see #executeRegion(Location, Runnable)
     */
    ScheduledTask runRegion(Location location, Consumer<ScheduledTask> task);

    default ScheduledTask runRegion(Location location, Runnable task)
    {
        return runRegion(location, scheduledTask -> task.run());
    }

    /**
     * Runs work on the next tick of the region that owns the supplied chunk.
     *
     * @see #executeRegion(World, int, int, Runnable)
     */
    ScheduledTask runRegion(World world, int chunkX, int chunkZ, Consumer<ScheduledTask> task);

    default ScheduledTask runRegion(World world, int chunkX, int chunkZ, Runnable task)
    {
        return runRegion(world, chunkX, chunkZ, scheduledTask -> task.run());
    }

    /**
     * Runs work on a location's owning region after a tick delay.
     *
     * @see #executeRegion(Location, Runnable)
     */
    ScheduledTask runRegionLater(Location location, Consumer<ScheduledTask> task, long delayTicks);

    default ScheduledTask runRegionLater(Location location, Runnable task, long delayTicks)
    {
        return runRegionLater(location, scheduledTask -> task.run(), delayTicks);
    }

    /**
     * Runs work on a chunk's owning region after a tick delay.
     *
     * @see #executeRegion(World, int, int, Runnable)
     */
    ScheduledTask runRegionLater(World world, int chunkX, int chunkZ, Consumer<ScheduledTask> task, long delayTicks);

    default ScheduledTask runRegionLater(World world, int chunkX, int chunkZ, Runnable task, long delayTicks)
    {
        return runRegionLater(world, chunkX, chunkZ, scheduledTask -> task.run(), delayTicks);
    }

    /**
     * Runs repeating work on a location's owning region.
     *
     * @see #executeRegion(Location, Runnable)
     */
    ScheduledTask runRegionTimer(Location location, Consumer<ScheduledTask> task, long delayTicks, long periodTicks);

    default ScheduledTask runRegionTimer(Location location, Runnable task, long delayTicks, long periodTicks)
    {
        return runRegionTimer(location, scheduledTask -> task.run(), delayTicks, periodTicks);
    }

    /**
     * Runs repeating work on a chunk's owning region.
     *
     * @see #executeRegion(World, int, int, Runnable)
     */
    ScheduledTask runRegionTimer(World world, int chunkX, int chunkZ, Consumer<ScheduledTask> task, long delayTicks, long periodTicks);

    default ScheduledTask runRegionTimer(World world, int chunkX, int chunkZ, Runnable task, long delayTicks, long periodTicks)
    {
        return runRegionTimer(world, chunkX, chunkZ, scheduledTask -> task.run(), delayTicks, periodTicks);
    }

    /**
     * Executes work on the region that currently owns the entity.
     * Use this for player and entity state access, inventory changes, kicks,
     * teleports, passengers, potion effects, and other entity-bound work.
     * Paper runs the retired callback if the entity is removed before the task
     * can execute.
     *
     * @return true if Paper accepted the task
     */
    boolean executeEntity(Entity entity, Runnable task, @Nullable Runnable retired, long delayTicks);

    default boolean executeEntity(Entity entity, Runnable task, long delayTicks)
    {
        return executeEntity(entity, task, null, delayTicks);
    }

    /**
     * Runs work on the next tick of the entity's owning region.
     *
     * @see #executeEntity(Entity, Runnable, Runnable, long)
     */
    @Nullable
    ScheduledTask runEntity(Entity entity, Consumer<ScheduledTask> task, @Nullable Runnable retired);

    default @Nullable ScheduledTask runEntity(Entity entity, Runnable task)
    {
        return runEntity(entity, scheduledTask -> task.run(), null);
    }

    /**
     * Runs work on the entity's owning region after a tick delay.
     *
     * @see #executeEntity(Entity, Runnable, Runnable, long)
     */
    @Nullable
    ScheduledTask runEntityLater(Entity entity, Consumer<ScheduledTask> task, @Nullable Runnable retired, long delayTicks);

    default @Nullable ScheduledTask runEntityLater(Entity entity, Runnable task, long delayTicks)
    {
        return runEntityLater(entity, scheduledTask -> task.run(), null, delayTicks);
    }

    /**
     * Runs repeating work on the entity's owning region.
     *
     * @see #executeEntity(Entity, Runnable, Runnable, long)
     */
    @Nullable
    ScheduledTask runEntityTimer(Entity entity, Consumer<ScheduledTask> task, @Nullable Runnable retired, long delayTicks, long periodTicks);

    default @Nullable ScheduledTask runEntityTimer(Entity entity, Runnable task, long delayTicks, long periodTicks)
    {
        return runEntityTimer(entity, scheduledTask -> task.run(), null, delayTicks, periodTicks);
    }

    /**
     * Cancels all global-region tasks owned by Plex.
     */
    void cancelGlobalTasks();

    /**
     * Cancels all async tasks owned by Plex.
     */
    void cancelAsyncTasks();
}
