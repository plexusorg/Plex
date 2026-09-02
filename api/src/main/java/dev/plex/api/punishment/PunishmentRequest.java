package dev.plex.api.punishment;

import java.time.Instant;
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
 * @param endDate punishment end date, or {@code null} for punishments without an end date
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
        if ((type == PunishmentType.BAN || type == PunishmentType.TEMPBAN || type == PunishmentType.MUTE
                || type == PunishmentType.FREEZE) && endDate == null)
        {
            throw new IllegalArgumentException(type + " requires an end date");
        }
        if ((type == PunishmentType.KICK || type == PunishmentType.SMITE) && endDate != null)
        {
            throw new IllegalArgumentException(type + " must not have an end date");
        }
        if (endDate != null && !endDate.toInstant().isAfter(Instant.now()))
        {
            throw new IllegalArgumentException(type + " requires a future end date");
        }
    }
}
