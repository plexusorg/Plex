package dev.plex.punishment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.plex.api.punishment.PunishmentSource;
import dev.plex.api.punishment.PunishmentType;
import dev.plex.util.PlexUtils;
import dev.plex.util.TimeUtils;
import dev.plex.util.adapter.ZonedDateTimeAdapter;

import java.time.ZonedDateTime;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
public class Punishment
{
    private static final Gson gson = new GsonBuilder().registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeAdapter()).create();
    @NotNull
    private final UUID punished;
    private final UUID punisher;
    private PunishmentSource source;
    private String punisherReference;
    private transient String resolvedPunisherName;
    private transient String resolvedPunishedName;
    private String ip;
    private PunishmentType type;
    private String reason;
    private volatile boolean active;
    private ZonedDateTime issueDate;
    private ZonedDateTime endDate;

    public Punishment(UUID punished, UUID punisher)
    {
        this.punished = punished;
        this.punisher = punisher;
        this.source = punisher == null ? PunishmentSource.CONSOLE : PunishmentSource.PLAYER;
        this.issueDate = ZonedDateTime.now(TimeUtils.zoneId());
    }

    public static Component generateBanMessage(Punishment punishment, String banUrl)
    {
        return PlexUtils.messageComponent("banMessage", banUrl, punishment.getReason(), endDate(punishment), punisherDisplayName(punishment));
    }

    public static Component generateAdmissionBanMessage(Punishment punishment, String banUrl)
    {
        String punisher = switch (punishment.getSource())
        {
            case PLAYER -> punishment.getResolvedPunisherName() == null || punishment.getResolvedPunisherName().isBlank()
                    ? "unknown" : punishment.getResolvedPunisherName();
            case CONSOLE -> "CONSOLE";
            case WEB -> punishment.getPunisherReference() == null || punishment.getPunisherReference().isBlank()
                    ? "WEB" : punishment.getPunisherReference();
        };
        return PlexUtils.messageComponent("banMessage", banUrl, punishment.getReason(), endDate(punishment), punisher);
    }

    public static Component generateKickMessage(Punishment punishment)
    {
        return PlexUtils.messageComponent("kickMessage", punishment.getReason(), punisherDisplayName(punishment));
    }

    public static String punisherDisplayName(Punishment punishment)
    {
        PunishmentSource source = punishment.getSource();
        return switch (source)
        {
            case PLAYER -> punishment.getResolvedPunisherName() == null || punishment.getResolvedPunisherName().isBlank()
                    ? punishment.getPunisher().toString() : punishment.getResolvedPunisherName();
            case CONSOLE -> "CONSOLE";
            case WEB -> punishment.getPunisherReference() == null || punishment.getPunisherReference().isBlank() ? "WEB" : punishment.getPunisherReference();
        };
    }

    private static String endDate(Punishment punishment)
    {
        return punishment.getEndDate() == null ? "Never" : TimeUtils.useTimezone(punishment.getEndDate());
    }

    public static Component generateIndefBanMessageWithReason(String type, String banUrl, String reason)
    {
        return PlexUtils.messageComponent("indefBanMessageReason", type, banUrl, reason);
    }

    public static Component generateIndefBanMessage(String type, String banUrl)
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
