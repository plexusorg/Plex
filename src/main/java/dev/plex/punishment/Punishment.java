package dev.plex.punishment;

import com.google.common.collect.Lists;
import com.google.gson.Gson;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.google.gson.GsonBuilder;
import dev.plex.util.adapter.LocalDateTimeAdapter;
import lombok.Getter;
import lombok.Setter;

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
    private LocalDateTime endDate;

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
        return new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter()).create().toJson(this);
    }

    public static Punishment fromJson(String json)
    {
        return new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter()).create().fromJson(json, Punishment.class);
    }
}
