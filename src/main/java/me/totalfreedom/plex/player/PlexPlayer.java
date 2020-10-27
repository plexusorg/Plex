package me.totalfreedom.plex.player;

import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexed;
import lombok.Getter;
import lombok.Setter;
import me.totalfreedom.plex.storage.MongoConnection;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter

@Entity(value = "players", noClassnameStored = true)
public class PlexPlayer
{
    @Id
    private String id;

    @Indexed(options = @IndexOptions(unique = true))
    private String uuid;

    @Indexed
    private String name;

    private List<String> ips;

    private boolean muted;
    private boolean frozen;

    //insert Rank check

    public PlexPlayer(){}

    public PlexPlayer(UUID playerUUID)
    {
        this.uuid = playerUUID.toString();

        this.id = uuid.substring(0, 8);

        this.name = "";

        this.ips = new ArrayList<>();

        this.muted = false;
        this.frozen = false;
    }

}
