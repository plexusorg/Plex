package dev.plex.api.rollback;

import org.bukkit.command.CommandSender;

/**
 * Provides player rollback functions.
 */
public interface RollbackApi
{
    /**
     * Checks if Plex has an active rollback integration.
     *
     * @return {@code true} if rollback support is available
     */
    boolean isAvailable();

    /**
     * Rolls back changes made by a player.
     *
     * @param sender command sender receiving rollback output
     * @param playerName player name to roll back
     * @param seconds number of seconds to roll back
     * @return {@code true} when the rollback was accepted
     */
    boolean rollback(CommandSender sender, String playerName, int seconds);

    /**
     * Rolls back the last 24 hours of changes made by a player.
     *
     * @param sender command sender receiving rollback output
     * @param playerName player name to roll back
     * @return {@code true} when the rollback was accepted
     */
    default boolean rollbackLastDay(CommandSender sender, String playerName)
    {
        return rollback(sender, playerName, 86400);
    }
}
