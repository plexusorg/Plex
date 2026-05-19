package dev.plex.storage.repository;

import dev.plex.punishment.Punishment;
import dev.plex.punishment.PunishmentType;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface PunishmentRepository
{
    CompletableFuture<List<Punishment>> getPunishments();

    List<Punishment> getPunishments(UUID uuid);

    List<Punishment> getPunishments(String ip);

    CompletableFuture<Void> insertPunishment(Punishment punishment);

    void syncRemoveBan(UUID uuid);

    CompletableFuture<Void> updatePunishment(PunishmentType type, boolean active, UUID punished);

    CompletableFuture<Void> removeBan(UUID uuid);
}
