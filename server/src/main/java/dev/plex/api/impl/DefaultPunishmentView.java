package dev.plex.api.impl;

import dev.plex.api.punishment.PunishmentSource;
import dev.plex.api.punishment.PunishmentType;
import dev.plex.api.punishment.PunishmentView;
import dev.plex.player.PlayerNameResolver;
import dev.plex.punishment.Punishment;
import java.time.ZonedDateTime;
import java.util.UUID;

record DefaultPunishmentView(Punishment punishment, PlayerNameResolver playerNameResolver) implements PunishmentView
{
    @Override public UUID punished() { return punishment.getPunished(); }
    @Override public UUID punisher() { return punishment.getPunisher(); }
    @Override public PunishmentSource source() { return punishment.getSource() == null ? (punishment.getPunisher() == null ? PunishmentSource.CONSOLE : PunishmentSource.PLAYER) : punishment.getSource(); }
    @Override public String punisherReference() { return punishment.getPunisherReference(); }
    @Override public String punisherDisplayName() { return Punishment.punisherDisplayName(punishment, playerNameResolver); }
    @Override public String ip() { return punishment.getIp(); }
    @Override public PunishmentType type() { return PunishmentType.valueOf(punishment.getType().name()); }
    @Override public String reason() { return punishment.getReason(); }
    @Override public boolean customTime() { return punishment.isCustomTime(); }
    @Override public boolean active() { return punishment.isActive(); }
    @Override public ZonedDateTime issueDate() { return punishment.getIssueDate(); }
    @Override public ZonedDateTime endDate() { return punishment.getEndDate(); }
}
