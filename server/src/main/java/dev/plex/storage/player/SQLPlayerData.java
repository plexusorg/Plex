package dev.plex.storage.player;

import dev.plex.player.PlexPlayer;
import dev.plex.storage.StorageType;
import dev.plex.storage.repository.PlayerRepository;
import dev.plex.storage.repository.PunishmentRepository;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;

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
        return jdbi.withHandle(h -> h.createQuery("SELECT 1 FROM players WHERE uuid = :u")
                .bind("u", uuid.toString()).mapTo(Integer.class).findFirst().isPresent());
    }

    public boolean exists(String username)
    {
        return jdbi.withHandle(h -> h.createQuery("SELECT 1 FROM players WHERE LOWER(last_known_name) = LOWER(:n)")
                .bind("n", username).mapTo(Integer.class).findFirst().isPresent());
    }

    public PlexPlayer getByUUID(UUID uuid, boolean loadExtraData)
    {
        PlexPlayer player = jdbi.withHandle(h ->
            {
                return h.createQuery("SELECT * FROM players WHERE uuid = :u")
                        .bind("u", uuid.toString()).map((rs, ctx) -> mapPlayer(h, rs)).findFirst().orElse(null);
            });
        return loadExtraData(player, loadExtraData);
    }

    public String getNameByUUID(UUID uuid)
    {
        return jdbi.withHandle(h -> h.createQuery("SELECT last_known_name FROM players WHERE uuid = :u")
                .bind("u", uuid.toString()).mapTo(String.class).findFirst().orElse(null));
    }

    public PlexPlayer getByName(String username, boolean loadExtraData)
    {
        PlexPlayer player = jdbi.withHandle(h ->
            {
                return h.createQuery("SELECT * FROM players WHERE LOWER(last_known_name) = LOWER(:n) LIMIT 1")
                        .bind("n", username).map((rs, ctx) -> mapPlayer(h, rs)).findFirst().orElse(null);
            });
        return loadExtraData(player, loadExtraData);
    }

    public PlexPlayer getByIP(String ip)
    {
        PlexPlayer player = jdbi.withHandle(h ->
            {
                String uuid = h.createQuery("SELECT player_uuid FROM player_ips WHERE ip = :ip LIMIT 1")
                        .bind("ip", ip).mapTo(String.class).findFirst().orElse(null);
                if (uuid == null)
                {
                    return null;
                }
                return h.createQuery("SELECT * FROM players WHERE uuid = :u")
                        .bind("u", uuid).map((rs, ctx) -> mapPlayer(h, rs)).findFirst().orElse(null);
            });
        return loadExtraData(player, true);
    }

    public void update(PlexPlayer player)
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
                insertIps(h, player.getUuid().toString(), player.getIps());
            });
    }

    public void insert(PlexPlayer player)
    {
        update(player);
    }

    private PlexPlayer mapPlayer(Handle handle, java.sql.ResultSet result) throws java.sql.SQLException
    {
        String uuid = result.getString("uuid");
        PlexPlayer player = new PlexPlayer(UUID.fromString(uuid));
        player.setName(result.getString("last_known_name"));
        player.setLoginMessage(result.getString("login_msg"));
        player.setPrefix(result.getString("prefix"));
        player.setStaffChat(result.getBoolean("staffChat"));
        player.setIps(loadIps(handle, uuid));
        player.setCommandSpy(result.getBoolean("commandspy"));
        return player;
    }

    private List<String> loadIps(Handle h, String uuid)
    {
        return h.createQuery("SELECT ip FROM player_ips WHERE player_uuid = :u")
                .bind("u", uuid).mapTo(String.class).list();
    }

    private void insertIps(Handle h, String playerUuid, List<String> ips)
    {
        for (String ip : ips.stream().filter(value -> value != null && !value.isBlank()).distinct().toList())
        {
            h.createUpdate(storageType.playerIpInsertSql())
                    .bind("u", playerUuid).bind("ip", ip).execute();
        }
    }

    private PlexPlayer loadExtraData(PlexPlayer player, boolean load)
    {
        if (player != null && load)
        {
            player.setPunishments(punishmentRepository.getPunishments(player.getUuid()));
        }
        return player;
    }
}
