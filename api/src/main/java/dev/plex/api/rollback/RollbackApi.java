package dev.plex.api.rollback;

import org.bukkit.command.CommandSender;

public interface RollbackApi
{
    boolean isAvailable();

    boolean rollback(CommandSender sender, String playerName, int seconds);

    default boolean rollbackLastDay(CommandSender sender, String playerName)
    {
        return rollback(sender, playerName, 86400);
    }
}
