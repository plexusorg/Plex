package dev.plex.player;

import com.google.common.collect.Lists;
import com.google.gson.GsonBuilder;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.IndexOptions;
import dev.morphia.annotations.Indexed;
import dev.plex.Plex;
import dev.plex.permission.Permission;
import dev.plex.punishment.Punishment;
import dev.plex.punishment.extra.Note;
import dev.plex.rank.enums.Rank;
import dev.plex.storage.StorageType;
import dev.plex.util.adapter.ZonedDateTimeAdapter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity(value = "players", useDiscriminator = false)
public class PlexPlayer
{
    @Setter(AccessLevel.NONE)
    @Id
    private String id;

    @Setter(AccessLevel.NONE)
    @Indexed(options = @IndexOptions(unique = true))
    private UUID uuid;

    @Indexed
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

    private boolean adminActive;

    private long coins;

    private String rank;

    private List<String> ips = Lists.newArrayList();
    private List<Punishment> punishments = Lists.newArrayList();
    private List<Note> notes = Lists.newArrayList();
    private List<Permission> permissions = Lists.newArrayList();

    private transient PermissionAttachment permissionAttachment;

    public PlexPlayer()
    {
    }

    public PlexPlayer(UUID playerUUID, boolean loadExtraData)
    {
        this.uuid = playerUUID;

        this.id = uuid.toString().substring(0, 8);

        this.name = "";

        this.loginMessage = "";
        this.prefix = "";

        this.vanished = false;
        this.commandSpy = false;

        this.coins = 0;

        this.rank = "";
        if (loadExtraData)
        {
            this.loadPunishments();
            if (Plex.get().getStorageType() != StorageType.MONGODB)
            {
                this.permissions.addAll(Plex.get().getSqlPermissions().getPermissions(this.uuid));
            }
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

    public Rank getRankFromString()
    {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        if (rank.isEmpty() || !isAdminActive())
        {
            if (player.isOp())
            {
                return Rank.OP;
            }
            else
            {
                return Rank.NONOP;
            }
        }
        else
        {
            return Rank.valueOf(rank.toUpperCase());
        }
    }

    public void loadPunishments()
    {
        if (Plex.get().getStorageType() != StorageType.MONGODB)
        {
            this.setPunishments(Plex.get().getSqlPunishment().getPunishments(this.getUuid()));
        }
    }

    public void loadNotes()
    {
        if (Plex.get().getStorageType() != StorageType.MONGODB)
        {
            Plex.get().getSqlNotes().getNotes(this.getUuid());
        }
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
