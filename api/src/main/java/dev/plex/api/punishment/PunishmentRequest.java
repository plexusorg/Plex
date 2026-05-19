package dev.plex.api.punishment;

import java.time.ZonedDateTime;
import java.util.UUID;

public record PunishmentRequest(UUID punished, UUID punisher, String punisherName, String ip,
                                String punishedUsername, PunishmentType type, String reason,
                                boolean customTime, boolean active, ZonedDateTime endDate)
{
}
