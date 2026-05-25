package dev.plex.api.punishment;

import java.time.ZonedDateTime;
import java.util.UUID;

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
 * @param customTime whether the punishment uses a custom duration
 * @param active whether the punishment should start active
 * @param endDate punishment end date, or {@code null} for punishments without an end date
 */
public record PunishmentRequest(UUID punished, UUID punisher, PunishmentSource source,
                                String punisherReference, String ip, PunishmentType type,
                                String reason, boolean customTime, boolean active,
                                ZonedDateTime endDate)
{
}
