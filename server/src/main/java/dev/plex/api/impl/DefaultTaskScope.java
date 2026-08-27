package dev.plex.api.impl;

import dev.plex.api.scheduler.SchedulerApi;
import dev.plex.api.scheduler.TaskScope;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.Nullable;

final class DefaultTaskScope implements TaskScope
{
    private final SchedulerApi scheduler;
    private final Set<ScheduledTask> tasks = ConcurrentHashMap.newKeySet();
    private final Set<DefaultTaskScope> childScopes = ConcurrentHashMap.newKeySet();
    private volatile boolean active = true;

    DefaultTaskScope(SchedulerApi scheduler)
    {
        this.scheduler = scheduler;
    }

    @Override
    public TaskScope taskScope()
    {
        checkActive();
        DefaultTaskScope child = new DefaultTaskScope(this);
        childScopes.add(child);
        return child;
    }

    @Override
    public Executor asyncExecutor()
    {
        return this::runAsync;
    }

    @Override
    public void executeGlobal(Runnable task)
    {
        runGlobal(task);
    }

    @Override
    public ScheduledTask runGlobal(Consumer<ScheduledTask> task)
    {
        return track(scheduler.runGlobal(once(task)));
    }

    @Override
    public ScheduledTask runGlobalLater(Consumer<ScheduledTask> task, long delayTicks)
    {
        return track(scheduler.runGlobalLater(once(task), delayTicks));
    }

    @Override
    public ScheduledTask runGlobalTimer(Consumer<ScheduledTask> task, long delayTicks, long periodTicks)
    {
        return track(scheduler.runGlobalTimer(repeating(task), delayTicks, periodTicks));
    }

    @Override
    public ScheduledTask runAsync(Consumer<ScheduledTask> task)
    {
        return track(scheduler.runAsync(once(task)));
    }

    @Override
    public ScheduledTask runAsyncLater(Consumer<ScheduledTask> task, long delay, TimeUnit unit)
    {
        return track(scheduler.runAsyncLater(once(task), delay, unit));
    }

    @Override
    public ScheduledTask runAsyncTimer(Consumer<ScheduledTask> task, long delay, long period, TimeUnit unit)
    {
        return track(scheduler.runAsyncTimer(repeating(task), delay, period, unit));
    }

    @Override
    public void executeRegion(Location location, Runnable task)
    {
        runRegion(location, task);
    }

    @Override
    public void executeRegion(World world, int chunkX, int chunkZ, Runnable task)
    {
        runRegion(world, chunkX, chunkZ, task);
    }

    @Override
    public ScheduledTask runRegion(Location location, Consumer<ScheduledTask> task)
    {
        return track(scheduler.runRegion(location, once(task)));
    }

    @Override
    public ScheduledTask runRegion(World world, int chunkX, int chunkZ, Consumer<ScheduledTask> task)
    {
        return track(scheduler.runRegion(world, chunkX, chunkZ, once(task)));
    }

    @Override
    public ScheduledTask runRegionLater(Location location, Consumer<ScheduledTask> task, long delayTicks)
    {
        return track(scheduler.runRegionLater(location, once(task), delayTicks));
    }

    @Override
    public ScheduledTask runRegionLater(World world, int chunkX, int chunkZ, Consumer<ScheduledTask> task, long delayTicks)
    {
        return track(scheduler.runRegionLater(world, chunkX, chunkZ, once(task), delayTicks));
    }

    @Override
    public ScheduledTask runRegionTimer(Location location, Consumer<ScheduledTask> task, long delayTicks, long periodTicks)
    {
        return track(scheduler.runRegionTimer(location, repeating(task), delayTicks, periodTicks));
    }

    @Override
    public ScheduledTask runRegionTimer(World world, int chunkX, int chunkZ, Consumer<ScheduledTask> task, long delayTicks, long periodTicks)
    {
        return track(scheduler.runRegionTimer(world, chunkX, chunkZ, repeating(task), delayTicks, periodTicks));
    }

    @Override
    public boolean executeEntity(Entity entity, Runnable task, @Nullable Runnable retired, long delayTicks)
    {
        Consumer<ScheduledTask> action = once(scheduledTask -> task.run());
        Runnable retiredAction = retired == null ? null : guard(retired);
        ScheduledTask scheduledTask = delayTicks <= 0
                ? scheduler.runEntity(entity, action, retiredAction)
                : scheduler.runEntityLater(entity, action, retiredAction, delayTicks);
        return track(scheduledTask) != null;
    }

    @Override
    public @Nullable ScheduledTask runEntity(Entity entity, Consumer<ScheduledTask> task, @Nullable Runnable retired)
    {
        return track(scheduler.runEntity(entity, once(task), retired == null ? null : guard(retired)));
    }

    @Override
    public @Nullable ScheduledTask runEntityLater(Entity entity, Consumer<ScheduledTask> task, @Nullable Runnable retired, long delayTicks)
    {
        return track(scheduler.runEntityLater(entity, once(task), retired == null ? null : guard(retired), delayTicks));
    }

    @Override
    public @Nullable ScheduledTask runEntityTimer(Entity entity, Consumer<ScheduledTask> task, @Nullable Runnable retired, long delayTicks, long periodTicks)
    {
        return track(scheduler.runEntityTimer(entity, repeating(task), retired == null ? null : guard(retired), delayTicks, periodTicks));
    }

    @Override
    public void cancelAll()
    {
        active = false;
        childScopes.forEach(DefaultTaskScope::cancelAll);
        childScopes.clear();
        tasks.forEach(ScheduledTask::cancel);
        tasks.clear();
    }

    private Runnable guard(Runnable task)
    {
        checkActive();
        return () ->
        {
            if (active)
            {
                task.run();
            }
        };
    }

    private Consumer<ScheduledTask> once(Consumer<ScheduledTask> task)
    {
        checkActive();
        return scheduledTask ->
        {
            tasks.remove(scheduledTask);
            if (active)
            {
                task.accept(scheduledTask);
            }
        };
    }

    private Consumer<ScheduledTask> repeating(Consumer<ScheduledTask> task)
    {
        checkActive();
        return scheduledTask ->
        {
            if (active)
            {
                task.accept(scheduledTask);
            }
        };
    }

    private <T extends ScheduledTask> @Nullable T track(@Nullable T task)
    {
        if (task == null)
        {
            return null;
        }
        tasks.add(task);
        if (!active && tasks.remove(task))
        {
            task.cancel();
        }
        return task;
    }

    private void checkActive()
    {
        if (!active)
        {
            throw new IllegalStateException("Task group is closed");
        }
    }
}
