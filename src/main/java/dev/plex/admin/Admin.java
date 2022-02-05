package dev.plex.admin;

import dev.plex.rank.enums.Rank;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

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
     * Contains a #isCommandSpy and #setCommandSpy by lombok
     */
    private boolean commandSpy = true;

    /**
     * Returns if the admin has staff chat toggled or not
     * <br>
     * Contains a #isStaffChat and #setStaffChat by lombok
     */
    private boolean staffChat = false;

    /**
     * Creates an admin with the startig ADMIN rank
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
