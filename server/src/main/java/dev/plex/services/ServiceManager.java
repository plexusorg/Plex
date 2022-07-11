package dev.plex.services;

import com.google.common.collect.Lists;
import dev.plex.Plex;
import dev.plex.PlexBase;
import dev.plex.services.impl.AutoWipeService;
import dev.plex.services.impl.BanService;
import dev.plex.services.impl.CommandBlockerService;
import dev.plex.services.impl.GameRuleService;
import dev.plex.services.impl.TimingService;
import dev.plex.services.impl.UpdateCheckerService;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

public class ServiceManager implements PlexBase
{
    private final List<AbstractService> services = Lists.newArrayList();

    public ServiceManager()
    {
        registerService(new AutoWipeService());
        registerService(new BanService());
        registerService(new CommandBlockerService());
        registerService(new GameRuleService());
        registerService(new TimingService());

        if (plugin.config.getBoolean("enable-updates"))
        {
            registerService(new UpdateCheckerService());
        }
    }

    public void startServices()
    {
        for (AbstractService service : services)
        {
            startService(service);
        }
    }

    public void endServices()
    {
        services.forEach(this::endService);
    }

    public AbstractService getService(Class<? extends AbstractService> clazz)
    {
        return services.stream().filter(service -> service.getClass().isAssignableFrom(clazz)).findFirst().orElse(null);
    }

    public void startService(AbstractService service)
    {
        if (!service.isRepeating())
        {
            int time = service.repeatInSeconds();
            BukkitTask task;
            if (time == 0)
            {
                task = Bukkit.getScheduler().runTask(Plex.get(), service::run);
            }
            else
            {
                task = Bukkit.getScheduler().runTaskLater(Plex.get(), service::run, time);
            }
            service.setTaskId(task.getTaskId());
        }
        else if (service.isRepeating() && service.isAsynchronous())
        {
            BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(Plex.get(), service::run, 0, 20L * service.repeatInSeconds());
            service.setTaskId(task.getTaskId());
        }
        else if (service.isRepeating() && !service.isAsynchronous())
        {
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(Plex.get(), service::run, 0, 20L * service.repeatInSeconds());
            service.setTaskId(task.getTaskId());
        }
        if (!services.contains(service))
        {
            services.add(service);
        }
        service.onStart();
    }

    public void endService(AbstractService service, boolean remove)
    {
        Bukkit.getScheduler().cancelTask(service.getTaskId());
        service.onEnd();
        if (remove)
        {
            services.remove(service);
        }
    }

    public void endService(AbstractService service)
    {
        endService(service, false);
    }

    private void registerService(AbstractService service)
    {
        services.add(service);
    }

    public int serviceCount()
    {
        return services.size();
    }
}
