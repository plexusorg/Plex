package dev.plex.api.punishment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.Nullable;

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
    List<IndefiniteBanView> indefiniteBans();

    /**
     * Looks up an indefinite ban by UUID.
     *
     * @param uuid UUID to look up
     * @return matching indefinite ban, if one exists
     */
    Optional<IndefiniteBanView> indefiniteBanByUuid(UUID uuid);

    /**
     * Finds an indefinite ban by player name.
     *
     * @param name player name
     * @return matching ban, if one exists
     */
    Optional<IndefiniteBanView> indefiniteBanByName(String name);

    /**
     * Finds an indefinite ban by IP address.
     *
     * @param ip IP address
     * @return matching ban, if one exists
     */
    Optional<IndefiniteBanView> indefiniteBanByIp(String ip);

    /**
     * Checks if a player has an active ban.
     *
     * @param uuid player UUID
     * @return result of the check
     */
    CompletableFuture<Boolean> isBanned(UUID uuid);

    /**
     * Checks whether a player has an active UUID or current IP ban.
     *
     * @param uuid player UUID
     * @param ip current player IP, or {@code null} to check only the UUID
     * @return result of the check
     */
    CompletableFuture<Boolean> isBanned(UUID uuid, @Nullable String ip);

    /**
     * Removes active bans for a player.
     *
     * @param uuid player UUID
     * @return future containing whether an active ban was removed
     */
    CompletableFuture<Boolean> unban(UUID uuid);

    /**
     * Applies a punishment to a player.
     *
     * @param punishment punishment details
     * @return result that completes when storage is updated
     * @throws NullPointerException if the request is null
     */
    CompletableFuture<Void> punish(PunishmentRequest punishment);
}
