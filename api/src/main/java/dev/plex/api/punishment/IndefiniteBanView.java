package dev.plex.api.punishment;

import java.util.List;
import java.util.UUID;

/**
 * Read-only view of an indefinite ban entry.
 */
public interface IndefiniteBanView
{
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
}
