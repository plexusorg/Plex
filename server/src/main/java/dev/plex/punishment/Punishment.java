package dev.plex.punishment;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.plex.player.PlayerService;
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
    // Optional display attribution for punishers without a Minecraft UUID
    // (e.g. web staff signed in via XenForo). When non-null, render this in
    // place of the UUID-based name lookup.
    private String punisherName;
    private String ip;
    private String punishedUsername;
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
        this.issueDate = ZonedDateTime.now(ZoneId.of(TimeUtils.TIMEZONE));
    }

    public static Component generateBanMessage(Punishment punishment, String banUrl, PlayerService playerService)
    {
        return PlexUtils.messageComponent("banMessage", banUrl, punishment.getReason(), TimeUtils.useTimezone(punishment.getEndDate()), punisherDisplayName(punishment, playerService));
    }

    public static Component generateKickMessage(Punishment punishment, PlayerService playerService)
    {
        return PlexUtils.messageComponent("kickMessage", punishment.getReason(), punisherDisplayName(punishment, playerService));
    }

    /**
     * Resolves the human-readable punisher attribution for display.
     * Prefers the explicit {@link #punisherName} (used for off-server
     * sources such as XenForo staff acting via the web HTTPD), falling
     * back to a UUID lookup, and finally "CONSOLE" when the punisher is
     * truly unknown.
     */
    public static String punisherDisplayName(Punishment punishment, PlayerService playerService)
    {
        String explicit = punishment.getPunisherName();
        if (explicit != null && !explicit.isEmpty()) return explicit;
        if (punishment.getPunisher() == null) return "CONSOLE";
        return playerService.getNameByUUID(punishment.getPunisher());
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
