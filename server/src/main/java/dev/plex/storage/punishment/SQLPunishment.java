package dev.plex.storage.punishment;

import dev.plex.api.punishment.PunishmentSource;
import dev.plex.punishment.Punishment;
import dev.plex.punishment.BanIpRange;
import dev.plex.punishment.TargetBan;
import dev.plex.api.punishment.PunishmentType;
import dev.plex.storage.repository.PunishmentRepository;
import dev.plex.storage.repository.PunishmentRepository.BanRemoval;
import dev.plex.util.PlexLog;
import dev.plex.util.TimeUtils;
import org.jdbi.v3.core.Jdbi;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
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
                        "AND (p.punished_uuid = :uuid OR (:ip <> '' AND p.ip_match_key = :ip)) " +
                        "AND ((p.endDate IS NOT NULL AND p.endDate > :now) " +
                        "OR (p.type = 'BAN' AND p.endDate IS NULL AND p.issueDate > :banCutoff)) " +
                        "ORDER BY CASE WHEN p.punished_uuid = :uuid THEN 0 ELSE 1 END, p.issueDate DESC LIMIT 1")
                .bind("active", true)
                .bind("uuid", uuid.toString())
                .bind("ip", BanIpRange.banMatchKey(canonicalIp))
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
                                "INSERT INTO punishments (punished_uuid, punisher_uuid, source, punisher_reference, ip, ip_match_key, type, reason, active, issueDate, endDate) " +
                                        "VALUES (:punishedUuid, :punisherUuid, :source, :punisherReference, :ip, :ipMatchKey, :type, :reason, :active, :issueDate, :endDate)")
                        .bind("punishedUuid", punishment.getPunished().toString())
                        .bind("punisherUuid", punishment.getPunisher() == null ? null : punishment.getPunisher().toString())
                        .bind("source", source.name())
                        .bind("punisherReference", punishment.getPunisherReference())
                        .bind("ip", punishment.getIp())
                        .bind("ipMatchKey", BanIpRange.banMatchKey(punishment.getIp()))
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
            List<String> ips = h.createQuery("SELECT DISTINCT ip_match_key FROM punishments WHERE punished_uuid = :u AND active = :active " +
                            "AND type IN ('BAN', 'TEMPBAN') AND ip_match_key <> ''")
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

    @Override
    public CompletableFuture<List<TargetBan>> loadActiveTargetBans(Instant now)
    {
        return CompletableFuture.supplyAsync(() -> jdbi.withHandle(h ->
        {
            List<TargetBan> bans = new ArrayList<>();
            for (TargetBan.Kind kind : TargetBan.Kind.values())
            {
                bans.addAll(h.createQuery("SELECT b.*, p.last_known_name AS resolved_punisher_name FROM " + targetBanTable(kind)
                                + " b LEFT JOIN players p ON p.uuid = b.punisher_uuid WHERE b.active = :active AND b.endDate > :now")
                        .bind("active", true).bind("now", now.toEpochMilli())
                        .map((rs, ctx) -> mapTargetBan(rs, kind)).list());
            }
            return List.copyOf(bans);
        }), executor);
    }

    @Override
    public CompletableFuture<Boolean> insertTargetBan(TargetBan ban)
    {
        return CompletableFuture.supplyAsync(() -> jdbi.inTransaction(h ->
        {
            // Serialize coverage checks across servers before reading active ranges.
            h.createUpdate("UPDATE target_ban_locks SET revision = revision + 1 WHERE kind = :kind")
                    .bind("kind", ban.kind().name()).execute();
            List<String> targets = h.createQuery("SELECT target FROM " + targetBanTable(ban.kind())
                            + " WHERE active = :active AND endDate > :now AND (:isIp = :active OR target = :target)")
                    .bind("active", true).bind("now", Instant.now().toEpochMilli())
                    .bind("isIp", ban.kind() == TargetBan.Kind.IP).bind("target", ban.target()).mapTo(String.class).list();
            for (String target : targets)
            {
                boolean duplicate = ban.kind() == TargetBan.Kind.NAME ? target.equals(ban.target())
                        : BanIpRange.parse(target).contains(ban.range());
                if (duplicate)
                {
                    return false;
                }
            }
            h.createUpdate("INSERT INTO " + targetBanTable(ban.kind())
                            + " (target, reason, punisher_uuid, source, punisher_reference, issueDate, endDate, active)"
                            + " VALUES (:target, :reason, :punisher, :source, :reference, :issueDate, :endDate, :active)")
                    .bind("target", ban.target()).bind("reason", ban.reason())
                    .bind("punisher", ban.punisher() == null ? null : ban.punisher().toString())
                    .bind("source", ban.source().name()).bind("reference", ban.punisherReference())
                    .bind("issueDate", ban.issuedAt().toEpochMilli()).bind("endDate", ban.endAt().toEpochMilli())
                    .bind("active", true).execute();
            return true;
        }), executor);
    }

    @Override
    public CompletableFuture<Boolean> deactivateTargetBan(TargetBan.Kind kind, String target, Instant now)
    {
        return CompletableFuture.supplyAsync(() -> jdbi.inTransaction(h ->
        {
            h.createUpdate("UPDATE target_ban_locks SET revision = revision + 1 WHERE kind = :kind")
                    .bind("kind", kind.name()).execute();
            return h.createUpdate("UPDATE " + targetBanTable(kind)
                            + " SET active = :inactive WHERE target = :target AND active = :active AND endDate > :now")
                    .bind("inactive", false).bind("target", target).bind("active", true)
                    .bind("now", now.toEpochMilli()).execute() > 0;
        }), executor);
    }

    private String targetBanTable(TargetBan.Kind kind)
    {
        return switch (kind)
        {
            case IP -> "ip_bans";
            case NAME -> "name_bans";
        };
    }

    private TargetBan mapTargetBan(ResultSet result, TargetBan.Kind kind) throws SQLException
    {
        String target = result.getString("target");
        String punisher = result.getString("punisher_uuid");
        return new TargetBan(kind, target, result.getString("reason"), punisher == null ? null : UUID.fromString(punisher),
                PunishmentSource.valueOf(result.getString("source")), result.getString("punisher_reference"),
                Instant.ofEpochMilli(result.getLong("issueDate")), Instant.ofEpochMilli(result.getLong("endDate")),
                kind == TargetBan.Kind.IP ? BanIpRange.parse(target) : null, result.getString("resolved_punisher_name"));
    }

    private Punishment mapPunishment(ResultSet result) throws SQLException
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
