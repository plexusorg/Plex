package dev.plex.punishment;

import com.google.common.collect.Lists;
import com.google.gson.GsonBuilder;
import dev.plex.Plex;
import dev.plex.util.MojangUtils;
import dev.plex.util.PlexUtils;
import dev.plex.util.adapter.LocalDateTimeDeserializer;
import dev.plex.util.adapter.LocalDateTimeSerializer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.Component;

@Getter
@Setter
public class Punishment
{
    private static final String banUrl = Plex.get().config.getString("banning.ban_url");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy 'at' hh:mm:ss a");
    private final UUID punished;
    private final UUID punisher;
    private final List<String> IPS;
    private String punishedUsername;
    private PunishmentType type;
    private String reason;
    private boolean customTime;
    private boolean active; // Field is only for bans
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

    public static Component generateBanMessage(Punishment punishment)
    {
        return PlexUtils.messageComponent("banMessage", banUrl, punishment.getReason(),
                DATE_FORMAT.format(punishment.getEndDate()), punishment.getPunisher() == null ? "CONSOLE" : MojangUtils.getInfo(punishment.getPunisher().toString()).getUsername());
    }

    public static Component generateIndefBanMessage(String type)
    {
        return PlexUtils.messageComponent("indefBanMessage", type, banUrl);
    }

    public static Punishment fromJson(String json)
    {
        return new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new LocalDateTimeDeserializer()).create().fromJson(json, Punishment.class);
    }

    public String toJSON()
    {
        return new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer()).create().toJson(this);
    }
}
