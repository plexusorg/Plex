package dev.plex.storage.player;

import dev.plex.player.PlexPlayer;
import dev.plex.storage.StorageType;
import dev.plex.storage.database.entity.PlayerEntity;
import dev.plex.storage.repository.PlayerRepository;
import dev.plex.storage.repository.PunishmentRepository;
import dev.plex.util.PlexLog;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.JdbiException;

import java.util.List;
import java.util.UUID;

/**
 * Player persistence backed by JDBI.
 */
public class SQLPlayerData implements PlayerRepository
{
    private final Jdbi jdbi;
    private final PunishmentRepository punishmentRepository;
    private final StorageType storageType;

    public SQLPlayerData(Jdbi jdbi, PunishmentRepository punishmentRepository, StorageType storageType)
    {
        this.jdbi = jdbi;
        this.punishmentRepository = punishmentRepository;
        this.storageType = storageType;
    }

    public boolean exists(UUID uuid)
    {
        try
        {
            return jdbi.withHandle(h -> h.createQuery("SELECT 1 FROM players WHERE uuid = :u")
                    .bind("u", uuid.toString()).mapTo(Integer.class).findFirst().isPresent());
        }
        catch (JdbiException e)
        {
            PlexLog.warn("Failed to check player existence for {0}: {1}", uuid, e.getMessage());
            return false;
        }
    }

    public boolean exists(String username)
    {
        try
        {
            return jdbi.withHandle(h -> h.createQuery("SELECT 1 FROM players WHERE last_known_name = :n")
                    .bind("n", username).mapTo(Integer.class).findFirst().isPresent());
        }
        catch (JdbiException e)
        {
            PlexLog.warn("Failed to check player existence for {0}: {1}", username, e.getMessage());
            return false;
        }
    }

    public PlexPlayer getByUUID(UUID uuid, boolean loadExtraData)
    {
        try
        {
            return jdbi.withHandle(h ->
            {
                PlayerEntity e = h.createQuery("SELECT * FROM players WHERE uuid = :u")
                        .bind("u", uuid.toString()).map((rs, ctx) -> mapRow(rs)).findFirst().orElse(null);
                return toPlayer(h, e, loadExtraData);
            });
        }
        catch (JdbiException e)
        {
            PlexLog.warn("Failed to load player by UUID {0}: {1}", uuid, e.getMessage());
            return null;
        }
    }

    public String getNameByUUID(UUID uuid)
    {
        try
        {
            return jdbi.withHandle(h -> h.createQuery("SELECT last_known_name FROM players WHERE uuid = :u")
                    .bind("u", uuid.toString()).mapTo(String.class).findFirst().orElse(null));
        }
        catch (JdbiException e)
        {
            PlexLog.warn("Failed to load player name by UUID {0}: {1}", uuid, e.getMessage());
            return null;
        }
    }

    public PlexPlayer getByUUID(UUID uuid)
    {
        return getByUUID(uuid, true);
    }

    public PlexPlayer getByName(String username, boolean loadExtraData)
    {
        try
        {
            return jdbi.withHandle(h ->
            {
                PlayerEntity e = h.createQuery("SELECT * FROM players WHERE last_known_name = :n LIMIT 1")
                        .bind("n", username).map((rs, ctx) -> mapRow(rs)).findFirst().orElse(null);
                return toPlayer(h, e, loadExtraData);
            });
        }
        catch (JdbiException e)
        {
            PlexLog.warn("Failed to load player by name {0}: {1}", username, e.getMessage());
            return null;
        }
    }

    public PlexPlayer getByName(String username)
    {
        return getByName(username, true);
    }

    public PlexPlayer getByIP(String ip)
    {
        try
        {
            return jdbi.withHandle(h ->
            {
                String uuid = h.createQuery("SELECT player_uuid FROM player_ips WHERE ip = :ip LIMIT 1")
                        .bind("ip", ip).mapTo(String.class).findFirst().orElse(null);
                if (uuid == null)
                {
                    return null;
                }
                PlayerEntity e = h.createQuery("SELECT * FROM players WHERE uuid = :u")
                        .bind("u", uuid).map((rs, ctx) -> mapRow(rs)).findFirst().orElse(null);
                return toPlayer(h, e, true);
            });
        }
        catch (JdbiException e)
        {
            PlexLog.warn("Failed to load player by IP {0}: {1}", ip, e.getMessage());
            return null;
        }
    }

    public void update(PlexPlayer player)
    {
        try
        {
            jdbi.useTransaction(h ->
            {
                h.createUpdate(storageType.playerUpsertSql())
                        .bind("uuid", player.getUuid().toString())
                        .bind("name", player.getName())
                        .bind("login", player.getLoginMessage())
                        .bind("prefix", player.getPrefix())
                        .bind("staffChat", player.isStaffChat())
                        .bind("commandSpy", player.isCommandSpy())
                        .execute();
                syncIps(h, player.getUuid().toString(), player.getIps());
            });
        }
        catch (JdbiException e)
        {
            PlexLog.warn("Failed to update player {0}: {1}", player.getUuid(), e.getMessage());
        }
    }

    public void insert(PlexPlayer player)
    {
        update(player);
    }

    private static PlayerEntity mapRow(java.sql.ResultSet rs) throws java.sql.SQLException
    {
        PlayerEntity e = new PlayerEntity();
        e.setUuid(rs.getString("uuid"));
        e.setLastKnownName(rs.getString("last_known_name"));
        e.setLoginMessage(rs.getString("login_msg"));
        e.setPrefix(rs.getString("prefix"));
        e.setStaffChat(rs.getBoolean("staffChat"));
        e.setCommandSpy(rs.getBoolean("commandspy"));
        return e;
    }

    private List<String> loadIps(Handle h, String uuid)
    {
        return h.createQuery("SELECT ip FROM player_ips WHERE player_uuid = :u")
                .bind("u", uuid).mapTo(String.class).list();
    }

    private void syncIps(Handle h, String playerUuid, List<String> ips)
    {
        h.createUpdate("DELETE FROM player_ips WHERE player_uuid = :u").bind("u", playerUuid).execute();
        for (String ip : ips.stream().distinct().toList())
        {
            h.createUpdate("INSERT INTO player_ips (player_uuid, ip) VALUES (:u, :ip)")
                    .bind("u", playerUuid).bind("ip", ip).execute();
        }
    }

    private PlexPlayer toPlayer(Handle h, PlayerEntity entity, boolean loadExtraData)
    {
        if (entity == null)
        {
            return null;
        }

        PlexPlayer plexPlayer = new PlexPlayer(UUID.fromString(entity.getUuid()), false);
        plexPlayer.setName(entity.getLastKnownName());
        plexPlayer.setLoginMessage(entity.getLoginMessage());
        plexPlayer.setPrefix(entity.getPrefix());
        plexPlayer.setStaffChat(entity.isStaffChat());
        plexPlayer.setIps(loadIps(h, entity.getUuid()));
        plexPlayer.setCommandSpy(entity.isCommandSpy());
        if (loadExtraData)
        {
            plexPlayer.setPunishments(punishmentRepository.getPunishments(plexPlayer.getUuid()));
            plexPlayer.checkMutesAndFreeze();
        }
        return plexPlayer;
    }
}
