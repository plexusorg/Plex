package dev.plex.storage.punishment;

import com.google.common.collect.Lists;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.stmt.UpdateBuilder;
import dev.plex.api.punishment.PunishmentSource;
import dev.plex.punishment.Punishment;
import dev.plex.punishment.PunishmentType;
import dev.plex.storage.database.entity.PunishmentEntity;
import dev.plex.storage.repository.PunishmentRepository;
import dev.plex.util.PlexLog;
import dev.plex.util.TimeUtils;

import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

public class SQLPunishment implements PunishmentRepository
{
    private final Dao<PunishmentEntity, Long> punishments;
    private final Executor executor;

    public SQLPunishment(ConnectionSource connectionSource, Executor executor)
    {
        try
        {
            this.punishments = DaoManager.createDao(connectionSource, PunishmentEntity.class);
            this.executor = executor;
        }
        catch (SQLException e)
        {
            throw new IllegalStateException("Failed to create punishment DAO", e);
        }
    }

    public CompletableFuture<List<Punishment>> getPunishments()
    {
        return CompletableFuture.supplyAsync(() ->
        {
            try
            {
                return punishments.queryForAll().stream().map(this::toPunishment).toList();
            }
            catch (SQLException e)
            {
                e.printStackTrace();
                return Lists.newArrayList();
            }
        }, executor);
    }

    public List<Punishment> getPunishments(UUID uuid)
    {
        try
        {
            return punishments.queryForEq("punished_uuid", uuid.toString()).stream().map(this::toPunishment).toList();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            return Lists.newArrayList();
        }
    }

    public List<Punishment> getPunishments(String ip)
    {
        try
        {
            return punishments.queryForEq("ip", ip).stream().map(this::toPunishment).toList();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
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
                punishments.create(toEntity(punishment));
            }
            catch (SQLException e)
            {
                e.printStackTrace();
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
            UpdateBuilder<PunishmentEntity, Long> update = punishments.updateBuilder();
            update.updateColumnValue("active", active);
            update.where().eq("punished_uuid", punished.toString()).and().eq("type", type.name());
            update.update();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
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
