package dev.plex.punishment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.plex.api.punishment.PunishmentSource;
import dev.plex.player.PlayerNameResolver;
import dev.plex.util.PlexUtils;
import dev.plex.util.TimeUtils;
import dev.plex.util.adapter.ZonedDateTimeAdapter;

import java.time.ZoneId;
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
    private String ip;
    private PunishmentType type;
    private String reason;
    private boolean customTime;
    private boolean active; // Field is only for bans
    private ZonedDateTime issueDate;
    private ZonedDateTime endDate;

    public Punishment(UUID punished, UUID punisher)
    {
        this.punished = punished;
        this.punisher = punisher;
        this.source = punisher == null ? PunishmentSource.CONSOLE : PunishmentSource.PLAYER;
        this.issueDate = ZonedDateTime.now(ZoneId.of(TimeUtils.TIMEZONE));
    }

    public static Component generateBanMessage(Punishment punishment, String banUrl, PlayerNameResolver playerNameResolver)
    {
        return PlexUtils.messageComponent("banMessage", banUrl, punishment.getReason(), TimeUtils.useTimezone(punishment.getEndDate()), punisherDisplayName(punishment, playerNameResolver));
    }

    public static Component generateKickMessage(Punishment punishment, PlayerNameResolver playerNameResolver)
    {
        return PlexUtils.messageComponent("kickMessage", punishment.getReason(), punisherDisplayName(punishment, playerNameResolver));
    }

    public static String punisherDisplayName(Punishment punishment, PlayerNameResolver playerNameResolver)
    {
        PunishmentSource source = punishment.getSource();
        if (source == null)
        {
            source = punishment.getPunisher() == null ? PunishmentSource.CONSOLE : PunishmentSource.PLAYER;
        }
        return switch (source)
        {
            case PLAYER -> punishment.getPunisher() == null ? "CONSOLE" : playerNameResolver.resolve(punishment.getPunisher());
            case CONSOLE -> "CONSOLE";
            case WEB -> punishment.getPunisherReference() == null || punishment.getPunisherReference().isBlank() ? "WEB" : punishment.getPunisherReference();
        };
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
