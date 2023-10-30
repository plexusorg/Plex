package dev.plex.player;

import com.google.common.collect.Lists;
import com.google.gson.GsonBuilder;
import dev.plex.Plex;
import dev.plex.punishment.Punishment;
import dev.plex.punishment.extra.Note;
import dev.plex.storage.annotation.MapObjectList;
import dev.plex.storage.annotation.PrimaryKey;
import dev.plex.storage.annotation.SQLTable;
import dev.plex.storage.annotation.VarcharLimit;
import dev.plex.util.adapter.ZonedDateTimeAdapter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.jetbrains.annotations.NotNull;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@SQLTable("players")
public class PlexPlayer
{
    @Setter(AccessLevel.NONE)
    @PrimaryKey
    @NotNull
    private UUID uuid;

    @VarcharLimit(16)
    @NotNull
    private String name;

    private String loginMessage;
    private String prefix;

    private boolean staffChat;
    private boolean vanished;
    private boolean commandSpy;

    // These fields are transient so MongoDB doesn't automatically drop them in.
    private transient boolean frozen;
    private transient boolean muted;
    private transient boolean lockedUp;

    private long coins;

    private List<String> ips = Lists.newArrayList();

    @MapObjectList
    private List<Punishment> punishments = Lists.newArrayList();

    @MapObjectList
    private List<Note> notes = Lists.newArrayList();

    public PlexPlayer()
    {
    }

    public PlexPlayer(UUID playerUUID, boolean loadPunishments)
    {
        this.uuid = playerUUID;
        this.name = "";

        this.loginMessage = "";
        this.prefix = "";

        this.vanished = false;
        this.commandSpy = false;

        this.coins = 0;

        if (loadPunishments)
        {
            this.loadPunishments();
//            this.permissions.addAll(Plex.get().getSqlPermissions().getPermissions(this.uuid));
        }
    }

    public PlexPlayer(UUID playerUUID)
    {
        this(playerUUID, true);
    }

    public String displayName()
    {
        return PlainTextComponentSerializer.plainText().serialize(getPlayer().displayName());
    }

    public void loadPunishments()
    {
        this.setPunishments(Plex.get().getSqlPunishment().getPunishments(this.getUuid()));
    }

    public void loadNotes()
    {
        Plex.get().getSqlNotes().getNotes(this.getUuid());
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
