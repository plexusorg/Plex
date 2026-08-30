package dev.plex.services;

import dev.plex.Plex;
import dev.plex.services.impl.AutoWipeService;
import dev.plex.services.impl.UpdateCheckerService;
import dev.plex.util.GameRuleUtil;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;

public class ServiceManager
{
    private final Plex plugin;
    private final AutoWipeService autoWipe;
    private final UpdateCheckerService updateChecker;
    private ScheduledTask autoWipeTask;
    private ScheduledTask updateCheckerTask;

    public ServiceManager(Plex plugin)
    {
        this.plugin = plugin;
        this.autoWipe = new AutoWipeService(plugin);
        this.updateChecker = new UpdateCheckerService(plugin);
    }

    public void startServices()
    {
        plugin.getApi().scheduler().runGlobal(
                () -> Bukkit.getWorlds().forEach(world -> GameRuleUtil.apply(plugin, world)));
        if (autoWipe.shouldStart())
        {
            autoWipe.onStart();
            autoWipeTask = plugin.getApi().scheduler().runGlobalTimer(
                    autoWipe::run, 1L, 20L * autoWipe.repeatInSeconds());
        }
        if (updateChecker.shouldStart())
        {
            updateChecker.onStart();
            updateCheckerTask = Bukkit.getAsyncScheduler().runAtFixedRate(
                    plugin, updateChecker::run, 1, updateChecker.repeatInSeconds(), TimeUnit.SECONDS);
        }
    }

    public void endServices()
    {
        if (updateCheckerTask != null)
        {
            updateCheckerTask.cancel();
            updateCheckerTask = null;
        }
        if (autoWipeTask != null)
        {
            autoWipeTask.cancel();
            autoWipeTask = null;
            autoWipe.onEnd();
        }
    }

    public int serviceCount()
    {
        return (autoWipeTask == null ? 0 : 1) + (updateCheckerTask == null ? 0 : 1);
    }
}
