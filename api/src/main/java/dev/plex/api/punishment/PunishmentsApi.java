package dev.plex.api.punishment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import dev.plex.api.player.PlexPlayerView;

public interface PunishmentsApi
{
    List<? extends IndefiniteBanView> indefiniteBans();
    Optional<? extends IndefiniteBanView> indefiniteBanByUuid(UUID uuid);
    void punish(PlexPlayerView player, PunishmentRequest punishment);
}
