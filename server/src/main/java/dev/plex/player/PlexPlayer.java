package dev.plex.player;

import com.google.common.collect.Lists;
import com.google.gson.GsonBuilder;
import dev.plex.punishment.Punishment;
import dev.plex.punishment.PunishmentType;
import dev.plex.util.adapter.ZonedDateTimeAdapter;
import dev.plex.util.TimeUtils;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
public class PlexPlayer
{
    @Setter(AccessLevel.NONE)
    @NotNull
    private UUID uuid;

    @NotNull
    private String name;

    private String loginMessage;
    private String prefix;

    private boolean staffChat;
    private boolean commandSpy;

    // These fields are transient so MongoDB doesn't automatically drop them in.
    private transient volatile boolean frozen;
    private transient volatile boolean muted;
    private transient volatile boolean lockedUp;

    private List<String> ips = Lists.newArrayList();

    @Setter(AccessLevel.NONE)
    private List<Punishment> punishments = Lists.newArrayList();

    public PlexPlayer()
    {
    }

    public PlexPlayer(UUID playerUUID, boolean loadPunishments)
    {
        this.uuid = playerUUID;
        this.name = "";

        this.loginMessage = "";
        this.prefix = "";

        this.commandSpy = false;

        if (loadPunishments)
        {
            this.checkMutesAndFreeze();
        }
    }

    public PlexPlayer(UUID playerUUID)
    {
        this(playerUUID, true);
    }

    public void setPunishments(List<Punishment> punishments)
    {
        this.punishments = new ArrayList<>(punishments);
    }

    public String displayName()
    {
        return PlainTextComponentSerializer.plainText().serialize(getPlayer().displayName());
    }

    public void checkMutesAndFreeze()
    {
        final ZonedDateTime now = ZonedDateTime.now(TimeUtils.zoneId());
        this.muted = this.punishments.stream().filter(punishment -> punishment.getType() == PunishmentType.MUTE)
                .anyMatch(punishment -> punishment.isActive() && punishment.getEndDate() != null && now.isBefore(punishment.getEndDate()));
        this.frozen = this.punishments.stream().filter(punishment -> punishment.getType() == PunishmentType.FREEZE)
                .anyMatch(punishment -> punishment.isActive() && punishment.getEndDate() != null && now.isBefore(punishment.getEndDate()));
    }

    public String toJSON()
    {
        return new GsonBuilder().registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeAdapter()).create().toJson(this);
    }

    public Player getPlayer()
    {
        return Bukkit.getPlayer(this.uuid);
    }
}
