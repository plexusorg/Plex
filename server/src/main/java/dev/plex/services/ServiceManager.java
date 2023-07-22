package dev.plex.services;

import com.google.common.collect.Lists;
import dev.plex.Plex;
import dev.plex.services.impl.AutoWipeService;
import dev.plex.services.impl.BanService;
import dev.plex.services.impl.CommandBlockerService;
import dev.plex.services.impl.GameRuleService;
import dev.plex.services.impl.TimingService;
import dev.plex.services.impl.UpdateCheckerService;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;

public class ServiceManager
{
    private final List<AbstractService> services = Lists.newArrayList();

    public ServiceManager()
    {
        registerService(new AutoWipeService());
        registerService(new BanService());
        registerService(new CommandBlockerService());
        registerService(new GameRuleService());
        registerService(new TimingService());
        registerService(new UpdateCheckerService());
    }

    public void startServices()
    {
        services.forEach(this::startService);
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
            if (time == 0)
            {
                Bukkit.getGlobalRegionScheduler().run(Plex.get(), service::run);
            }
            else
            {
                Bukkit.getAsyncScheduler().runDelayed(Plex.get(), service::run, time, TimeUnit.SECONDS);
            }
        }
        else if (service.isRepeating() && service.isAsynchronous())
        {
            Bukkit.getAsyncScheduler().runAtFixedRate(Plex.get(), service::run, 1, service.repeatInSeconds(), TimeUnit.SECONDS);
        }
        else if (service.isRepeating() && !service.isAsynchronous())
        {
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(Plex.get(), service::run, 1, 20L * service.repeatInSeconds());
        }
        if (!services.contains(service))
        {
            services.add(service);
        }
        service.onStart();
    }

    public void endService(AbstractService service, boolean remove)
    {
        Bukkit.getGlobalRegionScheduler().cancelTasks(Plex.get());
        Bukkit.getAsyncScheduler().cancelTasks(Plex.get());
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
