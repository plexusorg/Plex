package dev.plex.api.impl;

import dev.plex.Plex;
import dev.plex.api.scheduler.SchedulerApi;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

final class DefaultSchedulerApi implements SchedulerApi
{
    private final Plex plugin;
    private final Executor asyncExecutor;

    DefaultSchedulerApi(Plex plugin)
    {
        this.plugin = plugin;
        this.asyncExecutor = task -> Bukkit.getAsyncScheduler().runNow(plugin, scheduledTask -> task.run());
    }

    @Override
    public Executor asyncExecutor()
    {
        return asyncExecutor;
    }

    @Override
    public void executeGlobal(Runnable task)
    {
        Bukkit.getGlobalRegionScheduler().execute(plugin, task);
    }

    @Override
    public ScheduledTask runGlobal(Consumer<ScheduledTask> task)
    {
        return Bukkit.getGlobalRegionScheduler().run(plugin, task);
    }

    @Override
    public ScheduledTask runGlobalLater(Consumer<ScheduledTask> task, long delayTicks)
    {
        return Bukkit.getGlobalRegionScheduler().runDelayed(plugin, task, delayTicks);
    }

    @Override
    public ScheduledTask runGlobalTimer(Consumer<ScheduledTask> task, long delayTicks, long periodTicks)
    {
        return Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, task, delayTicks, periodTicks);
    }

    @Override
    public ScheduledTask runAsync(Consumer<ScheduledTask> task)
    {
        return Bukkit.getAsyncScheduler().runNow(plugin, task);
    }

    @Override
    public ScheduledTask runAsyncLater(Consumer<ScheduledTask> task, long delay, TimeUnit unit)
    {
        return Bukkit.getAsyncScheduler().runDelayed(plugin, task, delay, unit);
    }

    @Override
    public ScheduledTask runAsyncTimer(Consumer<ScheduledTask> task, long delay, long period, TimeUnit unit)
    {
        return Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task, delay, period, unit);
    }

    @Override
    public void executeRegion(Location location, Runnable task)
    {
        Bukkit.getRegionScheduler().execute(plugin, location, task);
    }

    @Override
    public void executeRegion(World world, int chunkX, int chunkZ, Runnable task)
    {
        Bukkit.getRegionScheduler().execute(plugin, world, chunkX, chunkZ, task);
    }

    @Override
    public ScheduledTask runRegion(Location location, Consumer<ScheduledTask> task)
    {
        return Bukkit.getRegionScheduler().run(plugin, location, task);
    }

    @Override
    public ScheduledTask runRegion(World world, int chunkX, int chunkZ, Consumer<ScheduledTask> task)
    {
        return Bukkit.getRegionScheduler().run(plugin, world, chunkX, chunkZ, task);
    }

    @Override
    public ScheduledTask runRegionLater(Location location, Consumer<ScheduledTask> task, long delayTicks)
    {
        return Bukkit.getRegionScheduler().runDelayed(plugin, location, task, delayTicks);
    }

    @Override
    public ScheduledTask runRegionLater(World world, int chunkX, int chunkZ, Consumer<ScheduledTask> task, long delayTicks)
    {
        return Bukkit.getRegionScheduler().runDelayed(plugin, world, chunkX, chunkZ, task, delayTicks);
    }

    @Override
    public ScheduledTask runRegionTimer(Location location, Consumer<ScheduledTask> task, long delayTicks, long periodTicks)
    {
        return Bukkit.getRegionScheduler().runAtFixedRate(plugin, location, task, delayTicks, periodTicks);
    }

    @Override
    public ScheduledTask runRegionTimer(World world, int chunkX, int chunkZ, Consumer<ScheduledTask> task, long delayTicks, long periodTicks)
    {
        return Bukkit.getRegionScheduler().runAtFixedRate(plugin, world, chunkX, chunkZ, task, delayTicks, periodTicks);
    }

    @Override
    public boolean executeEntity(Entity entity, Runnable task, @Nullable Runnable retired, long delayTicks)
    {
        return entity.getScheduler().execute(plugin, task, retired, delayTicks);
    }

    @Override
    public @Nullable ScheduledTask runEntity(Entity entity, Consumer<ScheduledTask> task, @Nullable Runnable retired)
    {
        return entity.getScheduler().run(plugin, task, retired);
    }

    @Override
    public @Nullable ScheduledTask runEntityLater(Entity entity, Consumer<ScheduledTask> task, @Nullable Runnable retired, long delayTicks)
    {
        return entity.getScheduler().runDelayed(plugin, task, retired, delayTicks);
    }

    @Override
    public @Nullable ScheduledTask runEntityTimer(Entity entity, Consumer<ScheduledTask> task, @Nullable Runnable retired, long delayTicks, long periodTicks)
    {
        return entity.getScheduler().runAtFixedRate(plugin, task, retired, delayTicks, periodTicks);
    }

    @Override
    public void cancelGlobalTasks()
    {
        Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);
    }

    @Override
    public void cancelAsyncTasks()
    {
        Bukkit.getAsyncScheduler().cancelTasks(plugin);
    }
}
