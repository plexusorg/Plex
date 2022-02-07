package dev.plex.banning;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexed;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.RandomStringUtils;

/**
 * The ban object
 *
 * @see BanManager
 */
@Getter
@Setter
@Entity(value = "bans", useDiscriminator = false)
public class Ban
{
    /**
     * A constructor for Morphia, can't be used
     */
    private Ban()
    {
    }

    /**
     * Gets the id of the ban (first 8 characters of a UUID + random 6 letters)
     */
    @Setter(AccessLevel.NONE)
    @Id
    private String id;

    /**
     * The unique ID of the player who was banned
     */
    @Setter(AccessLevel.NONE)
    @Indexed(options = @IndexOptions(unique = true))
    private UUID uuid;

    /**
     * The unique ID of the person who banned the player (can be null)
     */
    @Indexed // have the banner be indexed in the future to get bans issued by a person
    private UUID banner;

    /**
     * The IP of the banned player
     */
    private String ip;

    /**
     * The reason for the ban
     */
    private String reason;

    /**
     * The end date for the ban
     */
    private LocalDateTime endDate;

    /**
     * Whether the ban is active or not
     */
    private boolean active;

    /**
     * Creates a ban object
     *
     * @param uuid    The unique ID of the player being banned
     * @param banner  The unique ID of the sender banning the player
     * @param ip      The IP of the player being banned
     * @param reason  The reason for the ban
     * @param endDate When the ban will expire
     */
    public Ban(UUID uuid, UUID banner, String ip, String reason, LocalDateTime endDate)
    {
        this(uuid.toString().substring(0, 8) + "-" + RandomStringUtils.randomAlphabetic(6),
                uuid,
                banner,
                ip,
                reason,
                endDate);
    }

    /**
     * Creates a ban object
     *
     * @param id      The custom ID of the ban
     * @param uuid    The unique ID of the player being banned
     * @param banner  The unique ID of the sender banning the player
     * @param ip      The IP of the player being banned
     * @param reason  The reason for the ban
     * @param endDate When the ban will expire
     */
    public Ban(String id, UUID uuid, UUID banner, String ip, String reason, LocalDateTime endDate)
    {
        this.uuid = uuid;
        this.id = id;
        this.banner = banner;
        this.ip = ip;
        this.reason = reason;
        this.endDate = endDate;
        this.active = true;
    }
}
