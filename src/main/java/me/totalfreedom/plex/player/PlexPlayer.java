package me.totalfreedom.plex.player;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexed;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import me.totalfreedom.plex.rank.enums.Rank;

@Getter
@Setter
@Entity(value = "players", noClassnameStored = true)
public class PlexPlayer
{
    @Setter(AccessLevel.NONE)
    @Id
    private String id;

    @Setter(AccessLevel.NONE)
    @Indexed(options = @IndexOptions(unique = true))
    private String uuid;

    @Indexed
    private String name;

    private String loginMSG;
    private String prefix;

    private long coins;

    private Rank rank;

    private List<String> ips;

    public PlexPlayer(UUID playerUUID)
    {
        this.uuid = playerUUID.toString();

        this.id = uuid.substring(0, 8);

        this.name = "";

        this.loginMSG = "";
        this.prefix = "";

        this.coins = 0;

        this.ips = new ArrayList<>();

        this.rank = null;
    }
}
