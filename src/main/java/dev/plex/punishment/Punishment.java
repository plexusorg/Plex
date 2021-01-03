package dev.plex.punishment;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class Punishment
{

    private final UUID punished;
    private final UUID punisher;

    private final List<String> IPS;

    private String punishedUsername;

    private PunishmentType type;
    private String reason;
    private boolean customTime;
    private Date endDate;

    public Punishment(UUID punished, UUID punisher)
    {
        this.punished = punished;
        this.punisher = punisher;
        this.IPS = Lists.newArrayList();

        this.punishedUsername = "";
        this.type = null;
        this.reason = "";
        this.customTime = false;
        this.endDate = null;
    }

    public String toJSON()
    {
        return new Gson().toJson(this);
    }

    public static Punishment fromJson(String json)
    {
        return new Gson().fromJson(json, Punishment.class);
    }

}
