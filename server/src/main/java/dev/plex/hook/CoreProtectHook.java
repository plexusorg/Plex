package dev.plex.hook;

import dev.plex.Plex;
import dev.plex.util.PlexLog;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import org.bukkit.plugin.Plugin;

public class CoreProtectHook
{
    private CoreProtectAPI coreProtectAPI;
    private boolean hasApi;

    public CoreProtectHook(Plex plex)
    {
        Plugin plugin = plex.getServer().getPluginManager().getPlugin("CoreProtect");

        // Check that CoreProtect is loaded
        if (!(plugin instanceof CoreProtect))
        {
            PlexLog.debug("Plugin was not CoreProtect.");
            return;
        }

        // Check that the API is enabled
        CoreProtectAPI coreProtectAPI = ((CoreProtect) plugin).getAPI();
        this.hasApi = coreProtectAPI.isEnabled();
        if (!hasApi)
        {
            PlexLog.debug("CoreProtect API was disabled.");
            return;
        }

        // Check that a compatible version of the API is loaded
        if (coreProtectAPI.APIVersion() < 9)
        {
            PlexLog.debug("CoreProtect API version is: {0}", coreProtectAPI.APIVersion());
            return;
        }
        this.coreProtectAPI = coreProtectAPI;
        this.coreProtectAPI.testAPI();
    }

    public boolean hasCoreProtect() {
        return hasApi;
    }

    public CoreProtectAPI coreProtectAPI()
    {
        return coreProtectAPI;
    }
}
