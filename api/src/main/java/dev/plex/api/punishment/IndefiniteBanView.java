package dev.plex.api.punishment;

import java.util.List;
import java.util.UUID;

/**
 * Read-only view of an indefinite ban entry.
 */
public interface IndefiniteBanView
{
    /**
     * Returns the label of the entry in {@code indefbans.yml}.
     *
     * @return entry label
     */
    String label();

    /**
     * Returns usernames covered by the ban.
     *
     * @return usernames covered by the ban
     */
    List<String> usernames();

    /**
     * Returns UUIDs covered by the ban.
     *
     * @return UUIDs covered by the ban
     */
    List<UUID> uuids();

    /**
     * Returns IP addresses covered by the ban.
     *
     * @return IP addresses covered by the ban
     */
    List<String> ips();

    /**
     * Returns the ban reason.
     *
     * @return ban reason
     */
    String reason();

    /**
     * Returns the MiniMessage text shown as the whole ban screen in place of the configured indefinite ban screen.
     *
     * @return ban screen message, or an empty string when the configured screen is shown
     */
    String message();
}
