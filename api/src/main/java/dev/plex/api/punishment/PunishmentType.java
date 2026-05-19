package dev.plex.api.punishment;

/**
 * Supported punishment types exposed through the API.
 */
public enum PunishmentType
{
    /**
     * Prevents chat or other messaging actions.
     */
    MUTE,

    /**
     * Freezes player movement.
     */
    FREEZE,

    /**
     * Permanently bans a player.
     */
    BAN,

    /**
     * Temporarily bans a player until an end date.
     */
    TEMPBAN,

    /**
     * Kicks a player from the server.
     */
    KICK,

    /**
     * Applies the smite action to a player.
     */
    SMITE
}
