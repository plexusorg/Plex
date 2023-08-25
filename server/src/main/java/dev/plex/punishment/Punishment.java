package dev.plex.punishment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.morphia.annotations.Entity;
import dev.plex.Plex;
import dev.plex.util.MojangUtils;
import dev.plex.util.PlexUtils;
import dev.plex.util.TimeUtils;
import dev.plex.util.adapter.ZonedDateTimeAdapter;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;

import java.time.ZonedDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
public class Punishment
{
    private static final Gson gson = new GsonBuilder().registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeAdapter()).create();
    private static final String banUrl = Plex.get().config.getString("banning.ban_url");
    private final UUID punished;
    private final UUID punisher;
    private String ip;
    private String punishedUsername;
    private PunishmentType type;
    private String reason;
    private boolean customTime;
    private boolean active; // Field is only for bans
    private ZonedDateTime endDate;

    public Punishment()
    {
        this.punished = null;
        this.punisher = null;
    }

    public Punishment(UUID punished, UUID punisher)
    {
        this.punished = punished;
        this.punisher = punisher;
    }

    public static Component generateBanMessage(Punishment punishment)
    {
        return PlexUtils.messageComponent("banMessage", banUrl, punishment.getReason(), TimeUtils.useTimezone(punishment.getEndDate()), punishment.getPunisher() == null ? "CONSOLE" : MojangUtils.getInfo(punishment.getPunisher().toString()).getUsername());
    }

    public static Component generateKickMessage(Punishment punishment)
    {
        return PlexUtils.messageComponent("kickMessage", punishment.getReason(), punishment.getPunisher() == null ? "CONSOLE" : MojangUtils.getInfo(punishment.getPunisher().toString()).getUsername());
    }

    public static Component generateIndefBanMessageWithReason(String type, String reason)
    {
        return PlexUtils.messageComponent("indefBanMessageReason", type, banUrl, reason);
    }

    public static Component generateIndefBanMessage(String type)
    {
        return PlexUtils.messageComponent("indefBanMessage", type, banUrl);
    }

    public static Punishment fromJson(String json)
    {
        return gson.fromJson(json, Punishment.class);
    }

    public String toJSON()
    {
        return gson.toJson(this);
    }
}
