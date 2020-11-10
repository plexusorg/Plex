package me.totalfreedom.plex.banning;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexed;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.RandomStringUtils;

import java.util.Date;
import java.util.UUID;

@Getter
@Setter
@Entity(value = "bans", useDiscriminator = false)
public class Ban
{

    @Setter(AccessLevel.NONE)
    @Id
    private String id;

    @Setter(AccessLevel.NONE)
    @Indexed(options = @IndexOptions(unique = true))
    private final UUID uuid;

    @Indexed // have the banner be indexed in the future to get bans issued by a person
    private UUID banner;

    private String ip;
    private String reason;
    private Date endDate;
    private boolean active;
    public Ban(UUID uuid, UUID banner, String ip, String reason, Date endDate)
    {
        this.uuid = uuid;
        this.id = uuid.toString().substring(0, 8) + "-" + RandomStringUtils.randomAlphabetic(6);
        this.banner = banner;
        this.ip = ip;
        this.reason = reason;
        this.endDate = endDate;
        this.active = true;
    }

    public Ban(String id, UUID uuid, UUID banner, String reason, Date endDate)
    {
        this.uuid = uuid;
        this.id = id;
        this.banner = banner;
        this.reason = reason;
        this.endDate = endDate;
        this.active = true;
    }


}
