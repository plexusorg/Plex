package dev.plex.hook;

import dev.plex.Plex;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.prism_mc.prism.paper.api.PrismPaperApi;

public class PrismHook
{
    private RegisteredServiceProvider<PrismPaperApi> provider;

    public PrismHook(Plex plex)
    {
        Plugin plugin = plex.getServer().getPluginManager().getPlugin("Prism");

        // Check that Prism is loaded
        if (plugin != null && !plugin.isEnabled())
        {
            return;
        }

        provider = Bukkit.getServicesManager().getRegistration(PrismPaperApi.class);
    }

    public boolean hasPrism()
    {
        return provider != null;
    }

    public PrismPaperApi getPrism()
    {
        return provider.getProvider();
    }
}

