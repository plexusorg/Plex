package dev.plex.services;

import org.bukkit.Bukkit;

import dev.plex.Plex;
import dev.plex.services.impl.AutoWipeService;
import dev.plex.services.impl.UpdateCheckerService;
import dev.plex.util.GameRuleUtil;

public class ServiceManager
{
    private final Plex plugin;
    private final AutoWipeService autoWipe;
    private final UpdateCheckerService updateChecker;

    public ServiceManager(Plex plugin)
    {
        this.plugin = plugin;
        this.autoWipe = new AutoWipeService(plugin);
        this.updateChecker = new UpdateCheckerService(plugin);
    }

    public void startServices()
    {
        Bukkit.getGlobalRegionScheduler().run(plugin,
                task -> Bukkit.getWorlds().forEach(world -> GameRuleUtil.apply(plugin, world)));
        autoWipe.start();
        updateChecker.start();
    }

    public void endServices()
    {
        updateChecker.stop();
        autoWipe.stop();
    }

    public void shutdownServices()
    {
        autoWipe.stop();
        updateChecker.close();
    }

    public int serviceCount()
    {
        return (autoWipe.isRunning() ? 1 : 0) + (updateChecker.isRunning() ? 1 : 0);
    }
}
