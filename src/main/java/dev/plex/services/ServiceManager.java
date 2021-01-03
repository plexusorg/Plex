package dev.plex.services;

import com.google.common.collect.Lists;
import dev.plex.Plex;
import dev.plex.services.impl.BanService;
import org.bukkit.Bukkit;

import java.util.List;

public class ServiceManager
{

    private final List<AbstractService> services = Lists.newArrayList();

    public ServiceManager()
    {
        registerService(new BanService());
    }

    public void startServices()
    {
        for (AbstractService service : services)
        {
            if (service.isAsynchronous())
            {
                Bukkit.getScheduler().runTaskTimerAsynchronously(Plex.get(), service::run, 0, 20 * service.repeatInSeconds());
            } else {
                Bukkit.getScheduler().runTaskTimer(Plex.get(), service::run, 0, 20 * service.repeatInSeconds());
            }
        }
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
