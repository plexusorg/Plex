package dev.plex.storage.punishment;

import dev.plex.api.punishment.PunishmentSource;
import dev.plex.punishment.Punishment;
import dev.plex.api.punishment.PunishmentType;
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
            PunishmentSource source = punishment.getSource() == null
                    ? (punishment.getPunisher() == null ? PunishmentSource.CONSOLE : PunishmentSource.PLAYER)
                    : punishment.getSource();
            jdbi.useHandle(h -> h.createUpdate(
                                "INSERT INTO punishments (punished_uuid, punisher_uuid, source, punisher_reference, ip, type, reason, active, issueDate, endDate) " +
                                        "VALUES (:punishedUuid, :punisherUuid, :source, :punisherReference, :ip, :type, :reason, :active, :issueDate, :endDate)")
                        .bind("punishedUuid", punishment.getPunished().toString())
                        .bind("punisherUuid", punishment.getPunisher() == null ? null : punishment.getPunisher().toString())
                        .bind("source", source.name())
                        .bind("punisherReference", punishment.getPunisherReference())
                        .bind("ip", punishment.getIp())
                        .bind("type", punishment.getType().name())
                        .bind("reason", punishment.getReason())
                        .bind("active", punishment.isActive())
                        .bind("issueDate", punishment.getIssueDate().toInstant().toEpochMilli())
                        .bind("endDate", punishment.getEndDate() == null ? null : punishment.getEndDate().toInstant().toEpochMilli())
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

    private Punishment mapPunishment(java.sql.ResultSet result) throws java.sql.SQLException
    {
        String punisherUuid = result.getString("punisher_uuid");
        UUID punisher = punisherUuid == null || punisherUuid.isBlank() ? null : UUID.fromString(punisherUuid);
        Punishment punishment = new Punishment(UUID.fromString(result.getString("punished_uuid")), punisher);
        punishment.setActive(result.getBoolean("active"));
        punishment.setType(PunishmentType.valueOf(result.getString("type")));
        String source = result.getString("source");
        punishment.setSource(source == null ? punishment.getSource() : PunishmentSource.valueOf(source));
        punishment.setPunisherReference(result.getString("punisher_reference"));
        punishment.setIssueDate(ZonedDateTime.ofInstant(Instant.ofEpochMilli(result.getLong("issueDate")), TimeUtils.zoneId()));
        long endDate = result.getLong("endDate");
        punishment.setEndDate(result.wasNull() ? null : ZonedDateTime.ofInstant(Instant.ofEpochMilli(endDate), TimeUtils.zoneId()));
        if (punishment.getType() == PunishmentType.BAN && punishment.getEndDate() == null)
        {
            punishment.setEndDate(punishment.getIssueDate().plus(PunishmentType.STANDARD_BAN_DURATION));
        }
        punishment.setReason(result.getString("reason"));
        punishment.setIp(result.getString("ip"));
        punishment.setResolvedPunisherName(result.getString("resolved_punisher_name"));
        punishment.setResolvedPunishedName(result.getString("resolved_punished_name"));
        return punishment;
    }
}
