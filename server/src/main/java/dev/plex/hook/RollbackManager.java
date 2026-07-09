package dev.plex.hook;

import dev.plex.Plex;
import dev.plex.api.rollback.RollbackApi;

import java.util.Collections;

import org.bukkit.command.CommandSender;

public class RollbackManager implements RollbackApi
{
    private final Plex plugin;

    public RollbackManager(Plex plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public boolean isAvailable()
    {
        return (plugin.getPrismHook() != null && plugin.getPrismHook().hasPrism())
                || (plugin.getCoreProtectHook() != null && plugin.getCoreProtectHook().hasCoreProtect());
    }

    @Override
    public boolean rollback(CommandSender sender, String playerName, int seconds)
    {
        if (plugin.getPrismHook() != null && plugin.getPrismHook().hasPrism())
        {
            plugin.getApi().scheduler().runGlobal(() -> plugin.getPrismHook().rollback(sender, playerName, seconds));
            return true;
        }

        if (plugin.getCoreProtectHook() != null && plugin.getCoreProtectHook().hasCoreProtect())
        {
            rollbackWithCoreProtect(playerName, seconds);
            return true;
        }

        return false;
    }

    private void rollbackWithCoreProtect(String playerName, int seconds)
    {
        plugin.getApi().scheduler().runAsync(() ->
                plugin.getCoreProtectHook().coreProtectAPI().performRollback(seconds, Collections.singletonList(playerName), null, null, null, null, 0, null));
    }
}
