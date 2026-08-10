package dev.plex.api.impl;

import dev.plex.Plex;
import dev.plex.api.rollback.RollbackApi;
import dev.plex.hook.PrismHook;
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
        RollbackApi rollbackApi = rollbackApi();
        return rollbackApi != null && rollbackApi.isAvailable();
    }

    @Override
    public boolean rollback(CommandSender sender, String playerName, int seconds)
    {
        RollbackApi rollbackApi = rollbackApi();
        return rollbackApi != null && rollbackApi.rollback(sender, playerName, seconds);
    }

    private RollbackApi rollbackApi()
    {
        PrismHook prismHook = plugin.getPrismHook();
        return prismHook == null ? null : prismHook.getRollbackApi();
    }
}
