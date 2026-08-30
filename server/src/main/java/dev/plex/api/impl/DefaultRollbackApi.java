package dev.plex.api.impl;

import dev.plex.Plex;
import dev.plex.api.rollback.RollbackApi;
import dev.plex.hook.RollbackManager;
import org.bukkit.command.CommandSender;

final class DefaultRollbackApi implements RollbackApi
{
    private final RollbackApi rollback;

    DefaultRollbackApi(Plex plugin)
    {
        this.rollback = new RollbackManager(plugin);
    }

    @Override
    public boolean isAvailable()
    {
        return rollback.isAvailable();
    }

    @Override
    public boolean rollback(CommandSender sender, String playerName, int seconds)
    {
        return rollback.rollback(sender, playerName, seconds);
    }
}
