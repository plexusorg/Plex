package dev.plex.player;

import com.google.gson.GsonBuilder;
import dev.plex.punishment.Punishment;
import dev.plex.util.adapter.ZonedDateTimeAdapter;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

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

    private List<String> ips = new CopyOnWriteArrayList<>();

    @Setter(AccessLevel.NONE)
    private List<Punishment> punishments = new CopyOnWriteArrayList<>();

    public PlexPlayer()
    {
    }

    public PlexPlayer(UUID playerUUID)
    {
        this.uuid = playerUUID;
        this.name = "";

        this.loginMessage = "";
        this.prefix = "";

        this.commandSpy = false;
    }

    public void setPunishments(List<Punishment> punishments)
    {
        this.punishments = new CopyOnWriteArrayList<>(punishments);
    }

    public void setIps(List<String> ips)
    {
        this.ips = new CopyOnWriteArrayList<>(ips);
    }

    public String displayName()
    {
        return PlainTextComponentSerializer.plainText().serialize(getPlayer().displayName());
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
