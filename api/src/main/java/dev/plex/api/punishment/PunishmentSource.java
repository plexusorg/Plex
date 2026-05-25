package dev.plex.api.punishment;

/**
 * Source that issued a punishment.
 */
public enum PunishmentSource
{
    /**
     * Punishment issued by an in-game player.
     */
    PLAYER,
    /**
     * Punishment issued by the server console.
     */
    CONSOLE,
    /**
     * Punishment issued by a web or external integration.
     */
    WEB
}
