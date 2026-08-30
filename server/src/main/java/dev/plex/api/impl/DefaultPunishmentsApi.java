package dev.plex.api.impl;

import dev.plex.Plex;
import dev.plex.api.player.PlexPlayerView;
import dev.plex.api.punishment.IndefiniteBanView;
import dev.plex.api.punishment.PunishmentRequest;
import dev.plex.api.punishment.PunishmentsApi;
import dev.plex.player.PlexPlayer;
import dev.plex.punishment.Punishment;
import dev.plex.punishment.PunishmentType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

final class DefaultPunishmentsApi implements PunishmentsApi
{
    private final Plex plugin;

    DefaultPunishmentsApi(Plex plugin) { this.plugin = plugin; }

    @Override public List<? extends IndefiniteBanView> indefiniteBans() { return plugin.getPunishmentManager().getIndefiniteBans().stream().map(DefaultIndefiniteBanView::new).toList(); }
    @Override public Optional<? extends IndefiniteBanView> indefiniteBanByUuid(UUID uuid) { return Optional.ofNullable(plugin.getPunishmentManager().getIndefiniteBanByUUID(Objects.requireNonNull(uuid, "uuid"))).map(DefaultIndefiniteBanView::new); }
    @Override public Optional<? extends IndefiniteBanView> indefiniteBanByName(String name) { return Optional.ofNullable(plugin.getPunishmentManager().getIndefiniteBanByUsername(Objects.requireNonNull(name, "name"))).map(DefaultIndefiniteBanView::new); }
    @Override public Optional<? extends IndefiniteBanView> indefiniteBanByIp(String ip) { return Optional.ofNullable(plugin.getPunishmentManager().getIndefiniteBanByIP(Objects.requireNonNull(ip, "ip"))).map(DefaultIndefiniteBanView::new); }
    @Override public CompletionStage<Boolean> isBanned(UUID uuid) { return plugin.getPunishmentManager().isBanned(Objects.requireNonNull(uuid, "uuid")); }
    @Override public CompletionStage<Void> unban(UUID uuid) { return plugin.getPunishmentManager().unban(Objects.requireNonNull(uuid, "uuid")); }

    @Override
    public CompletableFuture<Void> punish(PlexPlayerView playerView, PunishmentRequest request)
    {
        Objects.requireNonNull(playerView, "playerView");
        Objects.requireNonNull(request, "request");
        if (!playerView.uuid().equals(request.punished()))
        {
            throw new IllegalArgumentException("The punishment UUID must match the player view UUID");
        }
        PlexPlayer player = DefaultPlayersApi.unwrap(playerView);
        if (player == null) player = plugin.getPlayerService().getPlayer(playerView.uuid());
        if (player == null)
        {
            throw new IllegalArgumentException("The player is not known to Plex: " + playerView.uuid());
        }
        Punishment punishment = new Punishment(request.punished(), request.punisher());
        punishment.setSource(request.source());
        punishment.setPunisherReference(request.punisherReference());
        punishment.setIp(request.ip());
        punishment.setType(PunishmentType.valueOf(request.type().name()));
        punishment.setReason(request.reason());
        punishment.setCustomTime(request.customTime());
        punishment.setActive(request.active());
        punishment.setEndDate(request.endDate());
        return plugin.getPunishmentManager().punish(player, punishment);
    }
}
