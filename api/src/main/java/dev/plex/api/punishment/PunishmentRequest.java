package dev.plex.api.punishment;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * Request payload used to create a punishment for a player.
 *
 * @param punished UUID of the player being punished
 * @param punisher UUID of the actor issuing the punishment
 * @param source source that issued the punishment
 * @param punisherReference source-specific actor reference
 * @param ip IP address associated with the punished player
 * @param type punishment type to apply
 * @param reason punishment reason
 * @param endDate punishment end date, or {@code null} for instant punishments; fixed-duration types replace this value
 */
public record PunishmentRequest(UUID punished, @Nullable UUID punisher, PunishmentSource source,
                                @Nullable String punisherReference, @Nullable String ip, PunishmentType type,
                                String reason, @Nullable ZonedDateTime endDate)
{
    /**
     * Creates and validates a punishment request.
     */
    public PunishmentRequest
    {
        Objects.requireNonNull(punished, "punished");
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(reason, "reason");
        if (source == PunishmentSource.PLAYER && punisher == null)
        {
            throw new IllegalArgumentException("A player punishment must have a punisher UUID");
        }
        endDate = validateEndDate(type, endDate);
    }

    private static ZonedDateTime validateEndDate(PunishmentType type, @Nullable ZonedDateTime endDate)
    {
        Instant now = Instant.now();
        Duration fixedDuration = type.fixedDuration().orElse(null);
        if (fixedDuration != null)
        {
            return ZonedDateTime.ofInstant(now.plus(fixedDuration), ZoneOffset.UTC);
        }

        if (!type.requiresEndDate())
        {
            if (endDate != null)
            {
                throw new IllegalArgumentException(type + " must not have an end date");
            }
            return null;
        }

        if (endDate == null)
        {
            throw new IllegalArgumentException(type + " requires an end date");
        }
        if (!endDate.toInstant().isAfter(now))
        {
            throw new IllegalArgumentException(type + " requires a future end date");
        }

        Duration maximumDuration = type.maximumDuration().orElse(null);
        if (maximumDuration != null && endDate.toInstant().isAfter(now.plus(maximumDuration)))
        {
            throw new IllegalArgumentException(type + " exceeds its maximum duration");
        }
        return endDate;
    }
}
