package me.totalfreedom.plex.player;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexed;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import me.totalfreedom.plex.rank.Rank;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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

    private Rank rank;

    private List<String> ips;

    public PlexPlayer(){}

    public PlexPlayer(UUID playerUUID)
    {
        this.uuid = playerUUID.toString();

        this.id = uuid.substring(0, 8);

        this.name = "";

        this.loginMSG = "";
        this.prefix = "";

        this.ips = new ArrayList<>();

        this.rank = null;
    }

}
