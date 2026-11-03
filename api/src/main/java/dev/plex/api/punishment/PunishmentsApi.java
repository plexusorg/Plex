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
     * Finds an indefinite ban by its label in {@code indefbans.yml}.
     *
     * @param label entry label
     * @return matching ban, if one exists
     */
    Optional<IndefiniteBanView> indefiniteBanByLabel(String label);

    /**
     * Adds an entry to {@code indefbans.yml}. Indefinite ban writes apply one at a time in call order and complete on
     * a Plex I/O thread. The write starts from the file on disk and keeps the rest of it, including comments. When the
     * future completes with {@code true} the file has been replaced, and with {@code false} it is unchanged; either
     * way, logins are then checked against its contents, including edits made to it since it was last loaded.
     *
     * @param ban entry to add
     * @return future containing {@code false} if an entry with the label already exists; it fails, leaving the file
     *         and the enforced bans unchanged, if the file cannot be read, loaded, or written
     * @throws NullPointerException if the request is null
     * @throws IllegalArgumentException if the label is empty, longer than 122 characters, or contains a dot or a line
     *                                  break, or an IP is not a valid address, range, or wildcard
     */
    CompletableFuture<Boolean> createIndefiniteBan(IndefiniteBanRequest ban);

    /**
     * Replaces the reason, usernames, UUIDs and IPs of an entry in {@code indefbans.yml}. Indefinite ban writes apply
     * one at a time in call order and complete on a Plex I/O thread. The write starts from the file on disk, keeps the
     * rest of it, and keeps the comments on values that remain in block-style lists. When the future completes with
     * {@code true} the file has been replaced, and with {@code false} it is unchanged; either way, logins are then
     * checked against its contents, including edits made to it since it was last loaded.
     *
     * @param ban new contents of the entry with the same label
     * @return future containing {@code false} if no entry has the label; it fails, leaving the file and the enforced
     *         bans unchanged, if the file cannot be read, loaded, or written
     * @throws NullPointerException if the request is null
     * @throws IllegalArgumentException if the label is empty or contains a dot, or an IP is not a valid address, range,
     *                                  or wildcard
     */
    CompletableFuture<Boolean> updateIndefiniteBan(IndefiniteBanRequest ban);

    /**
     * Removes an entry from {@code indefbans.yml}. Indefinite ban writes apply one at a time in call order and
     * complete on a Plex I/O thread. The write starts from the file on disk and keeps the rest of it, including the
     * comments above the entry. When the future completes with {@code true} the file has been replaced, and with
     * {@code false} it is unchanged; either way, logins are then checked against its contents, including edits made to
     * it since it was last loaded, so the entry no longer blocks logins.
     *
     * @param label entry label
     * @return future containing {@code false} if no entry has the label; it fails, leaving the file and the enforced
     *         bans unchanged, if the file cannot be read, loaded, or written
     * @throws NullPointerException if the label is null
     * @throws IllegalArgumentException if the label is empty or contains a dot
     */
    CompletableFuture<Boolean> deleteIndefiniteBan(String label);

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
