package dev.plex.api.impl;

import dev.plex.Plex;
import dev.plex.api.rollback.RollbackApi;
import dev.plex.hook.RollbackManager;
import org.bukkit.command.CommandSender;

final class DefaultRollbackApi implements RollbackApi
{
    private final Plex plugin;

    DefaultRollbackApi(Plex plugin)
    {
        this.plugin = plugin;
    }

    @Override
    public boolean isAvailable()
    {
        RollbackManager rollbackManager = plugin.getRollbackManager();
        return rollbackManager != null && rollbackManager.isAvailable();
    }

    @Override
    public boolean rollback(CommandSender sender, String playerName, int seconds)
    {
        RollbackManager rollbackManager = plugin.getRollbackManager();
        return rollbackManager != null && rollbackManager.rollback(sender, playerName, seconds);
    }
}
