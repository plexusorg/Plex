package dev.plex.api.punishment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;
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
     * Finds an indefinite ban by player name.
     *
     * @param name player name
     * @return matching ban, if one exists
     */
    Optional<? extends IndefiniteBanView> indefiniteBanByName(String name);

    /**
     * Finds an indefinite ban by IP address.
     *
     * @param ip IP address
     * @return matching ban, if one exists
     */
    Optional<? extends IndefiniteBanView> indefiniteBanByIp(String ip);

    /**
     * Checks if a player has an active ban.
     *
     * @param uuid player UUID
     * @return result of the check
     */
    CompletionStage<Boolean> isBanned(UUID uuid);

    /**
     * Removes active bans for a player.
     *
     * @param uuid player UUID
     * @return result that completes when storage is updated
     */
    CompletionStage<Void> unban(UUID uuid);

    /**
     * Applies a punishment to a player.
     *
     * @param player player to punish
     * @param punishment punishment details
     * @throws IllegalArgumentException if the request is for a different player
     *         or Plex does not know the player
     */
    void punish(PlexPlayerView player, PunishmentRequest punishment);
}
