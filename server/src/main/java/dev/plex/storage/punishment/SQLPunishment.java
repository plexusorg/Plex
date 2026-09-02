package dev.plex.storage.punishment;

import dev.plex.api.punishment.PunishmentSource;
import dev.plex.punishment.Punishment;
import dev.plex.punishment.PunishmentType;
import dev.plex.storage.database.entity.PunishmentEntity;
import dev.plex.storage.repository.PunishmentRepository;
import dev.plex.storage.repository.PunishmentRepository.BanRemoval;
import dev.plex.util.PlexLog;
import dev.plex.util.TimeUtils;
import org.jdbi.v3.core.Jdbi;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class SQLPunishment implements PunishmentRepository
{
    private final Jdbi jdbi;
    private final Executor executor;

    public SQLPunishment(Jdbi jdbi, Executor executor)
    {
        this.jdbi = jdbi;
        this.executor = executor;
    }

    public CompletableFuture<List<Punishment>> getPunishments()
    {
        return CompletableFuture.supplyAsync(() -> jdbi.withHandle(h -> h.createQuery(
                        "SELECT p.*, punisher.last_known_name AS resolved_punisher_name, punished.last_known_name AS resolved_punished_name FROM punishments p " +
                                "LEFT JOIN players punisher ON punisher.uuid = p.punisher_uuid " +
                                "LEFT JOIN players punished ON punished.uuid = p.punished_uuid " +
                                "ORDER BY p.issueDate DESC, p.id DESC")
                .map((rs, ctx) -> mapPunishment(rs)).list()), executor);
    }

    @Override
    public CompletableFuture<Optional<Punishment>> getEffectiveBan(UUID uuid, String canonicalIp, Instant now)
    {
        return CompletableFuture.supplyAsync(() -> jdbi.withHandle(h -> h.createQuery(
                        "SELECT p.*, punisher.last_known_name AS resolved_punisher_name, punished.last_known_name AS resolved_punished_name FROM punishments p " +
                        "LEFT JOIN players punisher ON punisher.uuid = p.punisher_uuid " +
                        "LEFT JOIN players punished ON punished.uuid = p.punished_uuid " +
                        "WHERE p.active = :active AND p.type IN ('BAN', 'TEMPBAN') " +
                        "AND (p.punished_uuid = :uuid OR (:ip <> '' AND p.ip = :ip)) " +
                        "AND ((p.endDate IS NOT NULL AND p.endDate > :now) " +
                        "OR (p.type = 'BAN' AND p.endDate IS NULL AND p.issueDate > :banCutoff)) " +
                        "ORDER BY CASE WHEN p.punished_uuid = :uuid THEN 0 ELSE 1 END, p.issueDate DESC LIMIT 1")
                .bind("active", true)
                .bind("uuid", uuid.toString())
                .bind("ip", canonicalIp)
                .bind("now", now.toEpochMilli())
                .bind("banCutoff", now.minus(PunishmentType.STANDARD_BAN_DURATION).toEpochMilli())
                .map((rs, ctx) ->
                {
                    return mapPunishment(rs);
                })
                .findFirst()), executor);
    }

    public List<Punishment> getPunishments(UUID uuid)
    {
        return jdbi.withHandle(h -> h.createQuery(
                        "SELECT p.*, punisher.last_known_name AS resolved_punisher_name, punished.last_known_name AS resolved_punished_name FROM punishments p " +
                                "LEFT JOIN players punisher ON punisher.uuid = p.punisher_uuid " +
                        "LEFT JOIN players punished ON punished.uuid = p.punished_uuid WHERE p.punished_uuid = :u " +
                                "ORDER BY p.issueDate DESC, p.id DESC")
                .bind("u", uuid.toString()).map((rs, ctx) -> mapPunishment(rs)).list());
    }

    public List<Punishment> getPunishments(String ip)
    {
        return jdbi.withHandle(h -> h.createQuery(
                        "SELECT p.*, punisher.last_known_name AS resolved_punisher_name, punished.last_known_name AS resolved_punished_name FROM punishments p " +
                                "LEFT JOIN players punisher ON punisher.uuid = p.punisher_uuid " +
                        "LEFT JOIN players punished ON punished.uuid = p.punished_uuid WHERE p.ip = :ip " +
                                "ORDER BY p.issueDate DESC, p.id DESC")
                .bind("ip", ip).map((rs, ctx) -> mapPunishment(rs)).list());
    }

    public CompletableFuture<Void> insertPunishment(Punishment punishment)
    {
        return CompletableFuture.runAsync(() ->
        {
            PlexLog.debug("Persisting punishment for " + punishment.getPunished());
            PunishmentEntity e = toEntity(punishment);
            jdbi.useHandle(h -> h.createUpdate(
                                "INSERT INTO punishments (punished_uuid, punisher_uuid, source, punisher_reference, ip, type, reason, active, issueDate, endDate) " +
                                        "VALUES (:punishedUuid, :punisherUuid, :source, :punisherReference, :ip, :type, :reason, :active, :issueDate, :endDate)")
                        .bind("punishedUuid", e.getPunishedUuid())
                        .bind("punisherUuid", e.getPunisherUuid())
                        .bind("source", e.getSource())
                        .bind("punisherReference", e.getPunisherReference())
                        .bind("ip", e.getIp())
                        .bind("type", e.getType())
                        .bind("reason", e.getReason())
                        .bind("active", e.isActive())
                        .bind("issueDate", e.getIssueDate())
                        .bind("endDate", e.getEndDate())
                    .execute());
        }, executor);
    }

    public CompletableFuture<Void> updatePunishment(PunishmentType type, boolean active, UUID punished)
    {
        return CompletableFuture.runAsync(() -> setActive(punished, type, active), executor);
    }

    @Override
    public CompletableFuture<Void> expirePunishments(PunishmentType type, UUID punished, Instant now)
    {
        return CompletableFuture.runAsync(() -> jdbi.useHandle(h -> h.createUpdate(
                        "UPDATE punishments SET active = :inactive WHERE punished_uuid = :u AND type = :t " +
                        "AND active = :active AND endDate IS NOT NULL AND endDate <= :now")
                .bind("inactive", false).bind("u", punished.toString()).bind("t", type.name())
                .bind("active", true).bind("now", now.toEpochMilli()).execute()), executor);
    }

    public CompletableFuture<BanRemoval> removeBan(UUID uuid)
    {
        return CompletableFuture.supplyAsync(() -> jdbi.inTransaction(h ->
        {
            List<String> ips = h.createQuery("SELECT DISTINCT ip FROM punishments WHERE punished_uuid = :u AND active = :active " +
                            "AND type IN ('BAN', 'TEMPBAN') AND ip IS NOT NULL")
                    .bind("u", uuid.toString()).bind("active", true).mapTo(String.class).list();
            int changed = h.createUpdate("UPDATE punishments SET active = :active WHERE punished_uuid = :u AND type IN ('BAN', 'TEMPBAN') AND active = :currentlyActive")
                    .bind("active", false).bind("currentlyActive", true).bind("u", uuid.toString()).execute();
            return new BanRemoval(changed > 0, ips);
        }), executor);
    }

    private void setActive(UUID punished, PunishmentType type, boolean active)
    {
        jdbi.useHandle(h -> h.createUpdate(
                            "UPDATE punishments SET active = :active WHERE punished_uuid = :u AND type = :t")
                    .bind("active", active)
                    .bind("u", punished.toString())
                    .bind("t", type.name())
                .execute());
    }

    private static PunishmentEntity mapRow(java.sql.ResultSet rs) throws java.sql.SQLException
    {
        PunishmentEntity e = new PunishmentEntity();
        e.setId(rs.getLong("id"));
        e.setPunishedUuid(rs.getString("punished_uuid"));
        e.setPunisherUuid(rs.getString("punisher_uuid"));
        e.setSource(rs.getString("source"));
        e.setPunisherReference(rs.getString("punisher_reference"));
        e.setIp(rs.getString("ip"));
        e.setType(rs.getString("type"));
        e.setReason(rs.getString("reason"));
        e.setActive(rs.getBoolean("active"));
        e.setIssueDate(rs.getLong("issueDate"));
        long endDate = rs.getLong("endDate");
        e.setEndDate(rs.wasNull() ? null : endDate);
        return e;
    }

    private Punishment toPunishment(PunishmentEntity entity)
    {
        UUID punisher = entity.getPunisherUuid() == null || entity.getPunisherUuid().isBlank() ? null : UUID.fromString(entity.getPunisherUuid());
        Punishment punishment = new Punishment(UUID.fromString(entity.getPunishedUuid()), punisher);
        punishment.setActive(entity.isActive());
        punishment.setType(PunishmentType.valueOf(entity.getType()));
        punishment.setSource(entity.getSource() == null ? punishment.getSource() : PunishmentSource.valueOf(entity.getSource()));
        punishment.setPunisherReference(entity.getPunisherReference());
        punishment.setIssueDate(ZonedDateTime.ofInstant(Instant.ofEpochMilli(entity.getIssueDate()), TimeUtils.zoneId()));
        punishment.setEndDate(entity.getEndDate() == null ? null : ZonedDateTime.ofInstant(Instant.ofEpochMilli(entity.getEndDate()), TimeUtils.zoneId()));
        if (punishment.getType() == PunishmentType.BAN && punishment.getEndDate() == null)
        {
            punishment.setEndDate(punishment.getIssueDate().plus(PunishmentType.STANDARD_BAN_DURATION));
        }
        punishment.setReason(entity.getReason());
        punishment.setIp(entity.getIp());
        return punishment;
    }

    private Punishment mapPunishment(java.sql.ResultSet rs) throws java.sql.SQLException
    {
        Punishment punishment = toPunishment(mapRow(rs));
        punishment.setResolvedPunisherName(rs.getString("resolved_punisher_name"));
        punishment.setResolvedPunishedName(rs.getString("resolved_punished_name"));
        return punishment;
    }

    private PunishmentEntity toEntity(Punishment punishment)
    {
        PunishmentEntity entity = new PunishmentEntity();
        entity.setPunishedUuid(punishment.getPunished().toString());
        entity.setPunisherUuid(punishment.getPunisher() == null ? null : punishment.getPunisher().toString());
        PunishmentSource source = punishment.getSource() == null ? (punishment.getPunisher() == null ? PunishmentSource.CONSOLE : PunishmentSource.PLAYER) : punishment.getSource();
        entity.setSource(source.name());
        entity.setPunisherReference(punishment.getPunisherReference());
        entity.setIp(punishment.getIp());
        entity.setType(punishment.getType().name());
        entity.setReason(punishment.getReason());
        entity.setActive(punishment.isActive());
        entity.setIssueDate(punishment.getIssueDate().toInstant().toEpochMilli());
        entity.setEndDate(punishment.getEndDate() == null ? null : punishment.getEndDate().toInstant().toEpochMilli());
        return entity;
    }
}
