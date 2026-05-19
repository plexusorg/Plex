package dev.plex.api.rollback;

import org.bukkit.command.CommandSender;

/**
 * CoreProtect rollback operations exposed to modules.
 */
public interface RollbackApi
{
    /**
     * Returns whether rollback support is currently available.
     *
     * @return whether rollback support is currently available
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
