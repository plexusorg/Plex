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

final class DefaultPunishmentsApi implements PunishmentsApi
{
    private final Plex plugin;

    DefaultPunishmentsApi(Plex plugin) { this.plugin = plugin; }

    @Override public List<? extends IndefiniteBanView> indefiniteBans() { return plugin.getPunishmentManager().getIndefiniteBans().stream().map(DefaultIndefiniteBanView::new).toList(); }
    @Override public Optional<? extends IndefiniteBanView> indefiniteBanByUuid(UUID uuid) { return Optional.ofNullable(plugin.getPunishmentManager().getIndefiniteBanByUUID(uuid)).map(DefaultIndefiniteBanView::new); }

    @Override
    public void punish(PlexPlayerView playerView, PunishmentRequest request)
    {
        PlexPlayer player = DefaultPlayersApi.unwrap(playerView);
        if (player == null) player = plugin.getPlayerService().getPlayer(playerView.uuid());
        Punishment punishment = new Punishment(request.punished(), request.punisher());
        punishment.setPunisherName(request.punisherName());
        punishment.setIp(request.ip());
        punishment.setPunishedUsername(request.punishedUsername());
        punishment.setType(PunishmentType.valueOf(request.type().name()));
        punishment.setReason(request.reason());
        punishment.setCustomTime(request.customTime());
        punishment.setActive(request.active());
        punishment.setEndDate(request.endDate());
        plugin.getPunishmentManager().punish(player, punishment);
    }
}
