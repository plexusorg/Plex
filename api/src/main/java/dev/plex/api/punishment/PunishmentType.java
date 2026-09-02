package dev.plex.api.punishment;

import java.time.Duration;

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
     * Bans a player for the standard 24-hour duration.
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
    SMITE;

    /** Standard duration used by {@link #BAN}. */
    public static final Duration STANDARD_BAN_DURATION = Duration.ofHours(24);
}
