package dev.plex.storage.repository;

import dev.plex.punishment.Punishment;
import dev.plex.punishment.PunishmentType;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.time.Instant;
import java.util.Optional;

public interface PunishmentRepository
{
    CompletableFuture<List<Punishment>> getPunishments();

    CompletableFuture<Optional<Punishment>> getEffectiveBan(UUID uuid, String canonicalIp, Instant now);

    List<Punishment> getPunishments(UUID uuid);

    List<Punishment> getPunishments(String ip);

    CompletableFuture<Void> insertPunishment(Punishment punishment);

    CompletableFuture<Void> updatePunishment(PunishmentType type, boolean active, UUID punished);

    CompletableFuture<Void> expirePunishments(PunishmentType type, UUID punished, Instant now);

    CompletableFuture<List<String>> removeBan(UUID uuid);
}
