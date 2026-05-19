package dev.plex.api.punishment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import dev.plex.api.player.PlexPlayerView;

/**
 * Reads and creates Plex punishments.
 */
public interface PunishmentsApi
{
    /**
     * Returns current indefinite bans.
     *
     * @return current indefinite bans
     */
    List<? extends IndefiniteBanView> indefiniteBans();

    /**
     * Looks up an indefinite ban by UUID.
     *
     * @param uuid UUID to look up
     * @return matching indefinite ban, if one exists
     */
    Optional<? extends IndefiniteBanView> indefiniteBanByUuid(UUID uuid);

    /**
     * Applies a punishment to a player.
     *
     * @param player player to punish
     * @param punishment punishment details
     */
    void punish(PlexPlayerView player, PunishmentRequest punishment);
}
