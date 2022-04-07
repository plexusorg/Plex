package dev.plex.services;

import com.google.common.collect.Lists;
import dev.plex.Plex;
import dev.plex.services.impl.AutoWipeService;
import dev.plex.services.impl.BanService;
import dev.plex.services.impl.GameRuleService;
import dev.plex.services.impl.UpdateCheckerService;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public class ServiceManager
{
    private final List<AbstractService> services = Lists.newArrayList();

    public ServiceManager()
    {
        registerService(new BanService());
        registerService(new GameRuleService());
        registerService(new UpdateCheckerService());
        registerService(new AutoWipeService());
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
            BukkitTask task = Bukkit.getScheduler().runTask(Plex.get(), service::run);
            service.setTaskId(task.getTaskId());
        } else if (service.isRepeating() && service.isAsynchronous())
        {
            BukkitTask task = Bukkit.getScheduler().runTaskTimerAsynchronously(Plex.get(), service::run, 0, 20L * service.repeatInSeconds());
            service.setTaskId(task.getTaskId());
        } else if (service.isRepeating() && !service.isAsynchronous())
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
