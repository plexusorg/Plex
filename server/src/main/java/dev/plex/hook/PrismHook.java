package dev.plex.hook;

import dev.plex.Plex;
import network.darkhelmet.prism.api.PrismApi;
import org.bukkit.plugin.Plugin;

public class PrismHook
{
    private PrismApi prismApi;

    public PrismHook(Plex plex)
    {
        Plugin plugin = plex.getServer().getPluginManager().getPlugin("Prism");

        // Check that Prism is loaded
        if (!plugin.isEnabled())
        {
            return;
        }

        // Check that the API is enabled
        this.prismApi = (PrismApi) plugin;
    }

    public boolean hasPrism() {
        return prismApi != null;
    }

    public PrismApi prismApi()
    {
        return prismApi;
    }
}

