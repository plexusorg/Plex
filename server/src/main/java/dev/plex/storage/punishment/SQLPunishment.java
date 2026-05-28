package dev.plex.storage.punishment;

import com.google.common.collect.Lists;
import dev.plex.api.punishment.PunishmentSource;
import dev.plex.punishment.Punishment;
import dev.plex.punishment.PunishmentType;
import dev.plex.storage.database.entity.PunishmentEntity;
import dev.plex.storage.repository.PunishmentRepository;
import dev.plex.util.PlexLog;
import dev.plex.util.TimeUtils;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.JdbiException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
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
        return CompletableFuture.supplyAsync(() ->
        {
            try
            {
                return jdbi.withHandle(h -> h.createQuery("SELECT * FROM punishments")
                        .map((rs, ctx) -> mapRow(rs)).list()).stream().map(this::toPunishment).toList();
            }
            catch (JdbiException e)
            {
                PlexLog.warn("Failed to load punishments: {0}", e.getMessage());
                return Lists.newArrayList();
            }
        }, executor);
    }

    public List<Punishment> getPunishments(UUID uuid)
    {
        try
        {
            return jdbi.withHandle(h -> h.createQuery("SELECT * FROM punishments WHERE punished_uuid = :u")
                    .bind("u", uuid.toString()).map((rs, ctx) -> mapRow(rs)).list())
                    .stream().map(this::toPunishment).toList();
        }
        catch (JdbiException e)
        {
            PlexLog.warn("Failed to load punishments for {0}: {1}", uuid, e.getMessage());
            return Lists.newArrayList();
        }
    }

    public List<Punishment> getPunishments(String ip)
    {
        try
        {
            return jdbi.withHandle(h -> h.createQuery("SELECT * FROM punishments WHERE ip = :ip")
                    .bind("ip", ip).map((rs, ctx) -> mapRow(rs)).list())
                    .stream().map(this::toPunishment).toList();
        }
        catch (JdbiException e)
        {
            PlexLog.warn("Failed to load punishments for IP {0}: {1}", ip, e.getMessage());
            return Lists.newArrayList();
        }
    }

    public CompletableFuture<Void> insertPunishment(Punishment punishment)
    {
        return CompletableFuture.runAsync(() ->
        {
            try
            {
                PlexLog.debug("Persisting punishment for " + punishment.getPunished());
                PunishmentEntity e = toEntity(punishment);
                jdbi.useHandle(h -> h.createUpdate(
                                "INSERT INTO punishments (punished_uuid, punisher_uuid, source, punisher_reference, ip, type, reason, customTime, active, issueDate, endDate) " +
                                        "VALUES (:punishedUuid, :punisherUuid, :source, :punisherReference, :ip, :type, :reason, :customTime, :active, :issueDate, :endDate)")
                        .bind("punishedUuid", e.getPunishedUuid())
                        .bind("punisherUuid", e.getPunisherUuid())
                        .bind("source", e.getSource())
                        .bind("punisherReference", e.getPunisherReference())
                        .bind("ip", e.getIp())
                        .bind("type", e.getType())
                        .bind("reason", e.getReason())
                        .bind("customTime", e.isCustomTime())
                        .bind("active", e.isActive())
                        .bind("issueDate", e.getIssueDate())
                        .bind("endDate", e.getEndDate())
                        .execute());
            }
            catch (JdbiException e)
            {
                PlexLog.warn("Failed to persist punishment for {0}: {1}", punishment.getPunished(), e.getMessage());
            }
        }, executor);
    }

    public void syncRemoveBan(UUID uuid)
    {
        setActive(uuid, PunishmentType.BAN, false);
        setActive(uuid, PunishmentType.TEMPBAN, false);
    }

    public CompletableFuture<Void> updatePunishment(PunishmentType type, boolean active, UUID punished)
    {
        return CompletableFuture.runAsync(() -> setActive(punished, type, active), executor);
    }

    public CompletableFuture<Void> removeBan(UUID uuid)
    {
        return CompletableFuture.runAsync(() -> syncRemoveBan(uuid), executor);
    }

    private void setActive(UUID punished, PunishmentType type, boolean active)
    {
        try
        {
            jdbi.useHandle(h -> h.createUpdate(
                            "UPDATE punishments SET active = :active WHERE punished_uuid = :u AND type = :t")
                    .bind("active", active)
                    .bind("u", punished.toString())
                    .bind("t", type.name())
                    .execute());
        }
        catch (JdbiException e)
        {
            PlexLog.warn("Failed to update punishment state for {0}: {1}", punished, e.getMessage());
        }
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
        e.setCustomTime(rs.getBoolean("customTime"));
        e.setActive(rs.getBoolean("active"));
        e.setIssueDate(rs.getLong("issueDate"));
        e.setEndDate(rs.getLong("endDate"));
        return e;
    }

    private Punishment toPunishment(PunishmentEntity entity)
    {
        UUID punisher = entity.getPunisherUuid() == null || entity.getPunisherUuid().isBlank() ? null : UUID.fromString(entity.getPunisherUuid());
        Punishment punishment = new Punishment(UUID.fromString(entity.getPunishedUuid()), punisher);
        punishment.setActive(entity.isActive());
        punishment.setType(PunishmentType.valueOf(entity.getType()));
        punishment.setCustomTime(entity.isCustomTime());
        punishment.setSource(entity.getSource() == null ? punishment.getSource() : PunishmentSource.valueOf(entity.getSource()));
        punishment.setPunisherReference(entity.getPunisherReference());
        punishment.setIssueDate(ZonedDateTime.ofInstant(Instant.ofEpochMilli(entity.getIssueDate()), ZoneId.of(TimeUtils.TIMEZONE)));
        punishment.setEndDate(ZonedDateTime.ofInstant(Instant.ofEpochMilli(entity.getEndDate()), ZoneId.of(TimeUtils.TIMEZONE)));
        punishment.setReason(entity.getReason());
        punishment.setIp(entity.getIp());
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
        entity.setCustomTime(punishment.isCustomTime());
        entity.setActive(punishment.isActive());
        entity.setIssueDate(punishment.getIssueDate().toInstant().toEpochMilli());
        entity.setEndDate(punishment.getEndDate().toInstant().toEpochMilli());
        return entity;
    }
}
