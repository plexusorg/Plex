package dev.plex.api.punishment;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Contents of an indefinite ban entry in {@code indefbans.yml}.
 *
 * @param label label of the entry; non-empty, without dots, and unique in the file. A label used to create an entry
 *              also has at most 122 characters and no line breaks.
 * @param reason reason for the ban, shown on the ban screen unless a message is set, or an empty string for none
 * @param message MiniMessage text shown as the whole ban screen in place of the configured indefinite ban screen, or
 *                an empty string for the configured screen
 * @param usernames usernames covered by the ban, matched ignoring case
 * @param uuids UUIDs covered by the ban
 * @param ips IP addresses or ranges covered by the ban, as an address, CIDR range, or IPv4 wildcard such as
 *            {@code 192.0.2.*}
 */
public record IndefiniteBanRequest(String label, String reason, String message, List<String> usernames,
                                   List<UUID> uuids, List<String> ips)
{
    /**
     * Creates an indefinite ban request with immutable copies of the lists.
     *
     * @throws NullPointerException if any argument or list element is null
     */
    public IndefiniteBanRequest
    {
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(message, "message");
        usernames = List.copyOf(usernames);
        uuids = List.copyOf(uuids);
        ips = List.copyOf(ips);
    }
}
