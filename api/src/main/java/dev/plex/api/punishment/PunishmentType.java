package dev.plex.api.punishment;

import java.time.Duration;
import java.util.Optional;

/**
 * Supported punishment types exposed through the API.
 */
public enum PunishmentType
{
    /**
     * Prevents chat or other messaging actions.
     */
    MUTE(true, true, null, Duration.ofDays(7)),

    /**
     * Freezes player movement.
     */
    FREEZE(true, true, null, null),

    /**
     * Bans a player for the standard 24-hour duration.
     */
    BAN(true, true, Duration.ofHours(24), null),

    /**
     * Temporarily bans a player until an end date.
     */
    TEMPBAN(true, true, null, null),

    /**
     * Kicks a player from the server.
     */
    KICK(false, false, null, null),

    /**
     * Applies the smite action to a player.
     */
    SMITE(false, false, null, null);

    /** Standard duration used by {@link #BAN}. */
    public static final Duration STANDARD_BAN_DURATION = BAN.fixedDuration;

    private final boolean requiresEndDate;
    private final boolean startsActive;
    private final Duration fixedDuration;
    private final Duration maximumDuration;

    PunishmentType(boolean requiresEndDate, boolean startsActive, Duration fixedDuration, Duration maximumDuration)
    {
        this.requiresEndDate = requiresEndDate;
        this.startsActive = startsActive;
        this.fixedDuration = fixedDuration;
        this.maximumDuration = maximumDuration;
    }

    /** Whether this punishment must have a future end date. */
    public boolean requiresEndDate()
    {
        return requiresEndDate;
    }

    /** Whether a newly-created punishment represents ongoing state. */
    public boolean startsActive()
    {
        return startsActive;
    }

    /** A duration enforced for this type, regardless of a caller-supplied end date. */
    public Optional<Duration> fixedDuration()
    {
        return Optional.ofNullable(fixedDuration);
    }

    /** The longest permitted duration for this type, if it has a limit. */
    public Optional<Duration> maximumDuration()
    {
        return Optional.ofNullable(maximumDuration);
    }

    /** Whether this punishment denies admission to the server. */
    public boolean isBan()
    {
        return this == BAN || this == TEMPBAN;
    }
}
