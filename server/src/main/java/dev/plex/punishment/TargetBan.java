package dev.plex.punishment;

import dev.plex.api.punishment.PunishmentSource;
import dev.plex.api.punishment.PunishmentType;
import dev.plex.util.TimeUtils;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.UUID;

public record TargetBan(Kind kind, String target, String reason, UUID punisher, PunishmentSource source,
                        String punisherReference, Instant issuedAt, Instant endAt, BanIpRange range, String resolvedPunisherName)
{
    public enum Kind
    {
        IP,
        NAME
    }

    public Punishment asPunishment(UUID targetUuid)
    {
        Punishment punishment = new Punishment(targetUuid, punisher);
        punishment.setType(PunishmentType.BAN);
        punishment.setSource(source);
        punishment.setPunisherReference(punisherReference);
        punishment.setResolvedPunisherName(resolvedPunisherName);
        punishment.setReason(reason);
        punishment.setActive(true);
        punishment.setIssueDate(ZonedDateTime.ofInstant(issuedAt, TimeUtils.zoneId()));
        punishment.setEndDate(ZonedDateTime.ofInstant(endAt, TimeUtils.zoneId()));
        return punishment;
    }
}
