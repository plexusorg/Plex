package dev.plex.api.player;

import java.util.List;
import java.util.UUID;
import dev.plex.api.punishment.PunishmentView;
import org.bukkit.entity.Player;

/**
 * Read-only view of a Plex player.
 */
public interface PlexPlayerView
{
    /**
     * Returns the player's UUID.
     *
     * @return player UUID
     */
    UUID uuid();

    /**
     * Returns the current or last known player name.
     *
     * @return current or last known player name
     */
    String name();

    /**
     * Returns known IP addresses.
     *
     * @return immutable copy of known IP addresses
     */
    List<String> ips();

    /**
     * Returns the player's punishment history.
     *
     * @return punishment history for the player
     */
    List<? extends PunishmentView> punishments();

    /**
     * Returns whether the player is currently frozen.
     *
     * @return whether the player is currently frozen
     */
    boolean frozen();

    /**
     * Returns whether the player is currently muted.
     *
     * @return whether the player is currently muted
     */
    boolean muted();

    /**
     * Returns whether the player is currently locked up.
     *
     * @return whether the player is currently locked up
     */
    boolean lockedUp();

    /**
     * Returns the Bukkit player instance.
     *
     * @return Bukkit player instance, or {@code null} when the player is offline
     */
    Player bukkitPlayer();
}
