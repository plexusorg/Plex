package dev.plex.services;

import com.google.common.collect.Lists;
import dev.plex.Plex;
import dev.plex.services.impl.AutoWipeService;
import dev.plex.services.impl.BanService;
import dev.plex.services.impl.GameRuleService;
import dev.plex.services.impl.TimingService;
import dev.plex.services.impl.UpdateCheckerService;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;

public class ServiceManager
{
    private final Plex plugin;
    private final List<AbstractService> services = Lists.newArrayList();

    public ServiceManager(Plex plugin)
    {
        this.plugin = plugin;
        registerService(new AutoWipeService(plugin));
        registerService(new BanService(plugin));
        registerService(new GameRuleService(plugin));
        registerService(new TimingService(plugin));
        registerService(new UpdateCheckerService(plugin));
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
                Bukkit.getGlobalRegionScheduler().run(plugin, service::run);
            }
            else
            {
                Bukkit.getAsyncScheduler().runDelayed(plugin, service::run, time, TimeUnit.SECONDS);
            }
        }
        else if (service.isRepeating() && service.isAsynchronous())
        {
            Bukkit.getAsyncScheduler().runAtFixedRate(plugin, service::run, 1, service.repeatInSeconds(), TimeUnit.SECONDS);
        }
        else if (service.isRepeating() && !service.isAsynchronous())
        {
            Bukkit.getGlobalRegionScheduler().runAtFixedRate(plugin, service::run, 1, 20L * service.repeatInSeconds());
        }
        if (!services.contains(service))
        {
            services.add(service);
        }
        service.onStart();
    }

    public void endService(AbstractService service, boolean remove)
    {
        Bukkit.getGlobalRegionScheduler().cancelTasks(plugin);
        Bukkit.getAsyncScheduler().cancelTasks(plugin);
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
