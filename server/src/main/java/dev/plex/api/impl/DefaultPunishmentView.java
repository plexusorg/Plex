package dev.plex.api.impl;

import dev.plex.api.punishment.PunishmentType;
import dev.plex.api.punishment.PunishmentView;
import dev.plex.punishment.Punishment;
import java.time.ZonedDateTime;
import java.util.UUID;

record DefaultPunishmentView(Punishment punishment) implements PunishmentView
{
    @Override public UUID punished() { return punishment.getPunished(); }
    @Override public UUID punisher() { return punishment.getPunisher(); }
    @Override public String punisherName() { return punishment.getPunisherName(); }
    @Override public String ip() { return punishment.getIp(); }
    @Override public String punishedUsername() { return punishment.getPunishedUsername(); }
    @Override public PunishmentType type() { return PunishmentType.valueOf(punishment.getType().name()); }
    @Override public String reason() { return punishment.getReason(); }
    @Override public boolean customTime() { return punishment.isCustomTime(); }
    @Override public boolean active() { return punishment.isActive(); }
    @Override public ZonedDateTime issueDate() { return punishment.getIssueDate(); }
    @Override public ZonedDateTime endDate() { return punishment.getEndDate(); }
}
