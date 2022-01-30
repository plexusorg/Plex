package dev.plex.services;

import com.google.common.collect.Lists;
import dev.plex.Plex;
import dev.plex.services.impl.GameRuleService;
import java.util.List;
import org.bukkit.Bukkit;

public class ServiceManager
{
    private final List<AbstractService> services = Lists.newArrayList();

    public ServiceManager()
    {
        //registerService(new BanService());
        registerService(new GameRuleService());
    }

    public void startServices()
    {
        for (AbstractService service : services)
        {
            if (!service.isRepeating())
            {
                Bukkit.getScheduler().runTask(Plex.get(), service::run);
            }
            else if (service.isRepeating() && service.isAsynchronous())
            {
                Bukkit.getScheduler().runTaskTimerAsynchronously(Plex.get(), service::run, 0, 20 * service.repeatInSeconds());
            }
            else if (service.isRepeating() && !service.isAsynchronous())
            {
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
