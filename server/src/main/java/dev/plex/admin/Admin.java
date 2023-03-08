package dev.plex.admin;

import dev.plex.rank.enums.Rank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Admin object to handle cached admins
 */
@Getter
@Setter
public class Admin
{
    /**
     * Gets the unique ID of an admin (immutable)
     */
    @Setter(AccessLevel.NONE)
    private UUID uuid;

    /**
     * Gets the rank of the admin
     * <br>
     * Contains a #setRank and #getRank by lombok
     */
    private Rank rank;

    /**
     * Returns if the admin has command spy or not
     * <br>
     * Contains a #isCommandSpy and #setCommandSpy by Lombok
     */
    private boolean commandSpy = false;

    /**
     * Returns if the admin has admin chat toggled or not
     * <br>
     * Contains a #isAdminChat and #setAdminChat by Lombok
     */
    private boolean adminChat = false;

    /**
     * Creates an admin with the ADMIN rank as the default rank
     *
     * @param uuid
     * @see UUID
     * @see Rank
     */
    public Admin(UUID uuid)
    {
        this.uuid = uuid;
        this.rank = Rank.ADMIN;
    }
}
