package dev.plex.api.impl;

import dev.plex.Plex;
import dev.plex.api.punishment.IndefiniteBanView;
import dev.plex.api.punishment.PunishmentRequest;
import dev.plex.api.punishment.PunishmentsApi;
import dev.plex.punishment.Punishment;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

final class DefaultPunishmentsApi implements PunishmentsApi
{
    private final Plex plugin;

    DefaultPunishmentsApi(Plex plugin) { this.plugin = plugin; }

    @Override public List<IndefiniteBanView> indefiniteBans() { return plugin.getPunishmentManager().getIndefiniteBans().stream().<IndefiniteBanView>map(DefaultIndefiniteBanView::new).toList(); }
    @Override public Optional<IndefiniteBanView> indefiniteBanByUuid(UUID uuid) { return Optional.ofNullable(plugin.getPunishmentManager().getIndefiniteBanByUUID(Objects.requireNonNull(uuid, "uuid"))).map(DefaultIndefiniteBanView::new); }
    @Override public Optional<IndefiniteBanView> indefiniteBanByName(String name) { return Optional.ofNullable(plugin.getPunishmentManager().getIndefiniteBanByUsername(Objects.requireNonNull(name, "name"))).map(DefaultIndefiniteBanView::new); }
    @Override public Optional<IndefiniteBanView> indefiniteBanByIp(String ip) { return Optional.ofNullable(plugin.getPunishmentManager().getIndefiniteBanByIP(Objects.requireNonNull(ip, "ip"))).map(DefaultIndefiniteBanView::new); }
    @Override public CompletableFuture<Boolean> isBanned(UUID uuid) { return plugin.getPunishmentManager().isBanned(Objects.requireNonNull(uuid, "uuid")); }
    @Override public CompletableFuture<Boolean> isBanned(UUID uuid, String ip) { return plugin.getPunishmentManager().isBanned(Objects.requireNonNull(uuid, "uuid"), ip); }
    @Override public CompletableFuture<Boolean> unban(UUID uuid) { return plugin.getPunishmentManager().unban(Objects.requireNonNull(uuid, "uuid")); }

    @Override
    public CompletableFuture<Void> punish(PunishmentRequest request)
    {
        Objects.requireNonNull(request, "request");
        Punishment punishment = new Punishment(request.punished(), request.punisher());
        punishment.setSource(request.source());
        punishment.setPunisherReference(request.punisherReference());
        punishment.setIp(request.ip());
        punishment.setType(request.type());
        punishment.setReason(request.reason());
        punishment.setEndDate(request.endDate());
        return plugin.getPlayerService().findPlayer(request.punished()).thenCompose(player ->
        {
            if (player == null)
                return CompletableFuture.failedFuture(new IllegalArgumentException("The player is not known to Plex: " + request.punished()));
            return plugin.getPunishmentManager().punish(player, punishment);
        });
    }
}
