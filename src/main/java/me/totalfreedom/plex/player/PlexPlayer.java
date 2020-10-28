package me.totalfreedom.plex.player;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import me.totalfreedom.plex.rank.enums.Rank;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;

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

    private long coins;

    private String rank;

    private List<String> ips;

    public PlexPlayer(){}

    public PlexPlayer(UUID playerUUID)
    {
        this.uuid = playerUUID.toString();

        this.id = uuid.substring(0, 8);

        this.name = "";

        this.loginMSG = "";
        this.prefix = "";

        this.coins = 0;

        this.ips = new ArrayList<>();

        this.rank = "";
    }

    public Rank getRankFromString()
    {
        return Rank.valueOf(rank.toUpperCase());
    }

}
