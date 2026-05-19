package dev.plex.storage.player;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.stmt.DeleteBuilder;
import dev.plex.player.PlexPlayer;
import dev.plex.storage.database.entity.PlayerEntity;
import dev.plex.storage.database.entity.PlayerIpEntity;
import dev.plex.storage.repository.PlayerRepository;
import dev.plex.storage.repository.PunishmentRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Player persistence backed by ORMLite.
 */
public class SQLPlayerData implements PlayerRepository
{
    private static final Gson GSON = new Gson();
    private final Dao<PlayerEntity, String> players;
    private final Dao<PlayerIpEntity, Long> playerIps;
    private final PunishmentRepository punishmentRepository;

    public SQLPlayerData(ConnectionSource connectionSource, PunishmentRepository punishmentRepository)
    {
        this.punishmentRepository = punishmentRepository;
        try
        {
            this.players = DaoManager.createDao(connectionSource, PlayerEntity.class);
            this.playerIps = DaoManager.createDao(connectionSource, PlayerIpEntity.class);
        }
        catch (SQLException e)
        {
            throw new IllegalStateException("Failed to create player DAOs", e);
        }
    }

    public boolean exists(UUID uuid)
    {
        try
        {
            return players.idExists(uuid.toString());
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            return false;
        }
    }

    public boolean exists(String username)
    {
        try
        {
            return players.queryBuilder().where().eq("name", username).queryForFirst() != null;
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            return false;
        }
    }

    public PlexPlayer getByUUID(UUID uuid, boolean loadExtraData)
    {
        try
        {
            return toPlayer(players.queryForId(uuid.toString()), loadExtraData);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public String getNameByUUID(UUID uuid)
    {
        try
        {
            PlayerEntity entity = players.queryForId(uuid.toString());
            return entity == null ? null : entity.getName();
        }
        catch (SQLException e)
        {
            e.printStackTrace();
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
            return toPlayer(players.queryBuilder().limit(1L).where().eq("name", username).queryForFirst(), loadExtraData);
        }
        catch (SQLException e)
        {
            e.printStackTrace();
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
            PlayerIpEntity playerIp = playerIps.queryBuilder().limit(1L).where().eq("ip", ip).queryForFirst();
            if (playerIp != null)
            {
                return toPlayer(players.queryForId(playerIp.getPlayerUuid()), true);
            }

            for (PlayerEntity entity : players.queryForAll())
            {
                List<String> ips = parseIps(entity.getIps());
                if (ips.contains(ip))
                {
                    syncIps(entity.getUuid(), ips);
                    return toPlayer(entity, true);
                }
            }
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public void update(PlexPlayer player)
    {
        try
        {
            players.createOrUpdate(toEntity(player));
            syncIps(player.getUuid().toString(), player.getIps());
        }
        catch (SQLException e)
        {
            e.printStackTrace();
        }
    }

    public void insert(PlexPlayer player)
    {
        update(player);
    }

    private PlexPlayer toPlayer(PlayerEntity entity, boolean loadExtraData)
    {
        if (entity == null)
        {
            return null;
        }

        PlexPlayer plexPlayer = new PlexPlayer(UUID.fromString(entity.getUuid()), false);
        plexPlayer.setName(entity.getName());
        plexPlayer.setLoginMessage(entity.getLoginMessage());
        plexPlayer.setPrefix(entity.getPrefix());
        plexPlayer.setStaffChat(entity.isStaffChat());
        plexPlayer.setIps(parseIps(entity.getIps()));
        plexPlayer.setCoins(entity.getCoins());
        plexPlayer.setVanished(entity.isVanished());
        plexPlayer.setCommandSpy(entity.isCommandSpy());
        if (loadExtraData)
        {
            plexPlayer.setPunishments(punishmentRepository.getPunishments(plexPlayer.getUuid()));
            plexPlayer.checkMutesAndFreeze();
        }
        return plexPlayer;
    }

    private PlayerEntity toEntity(PlexPlayer player)
    {
        PlayerEntity entity = new PlayerEntity();
        entity.setUuid(player.getUuid().toString());
        entity.setName(player.getName());
        entity.setLoginMessage(player.getLoginMessage());
        entity.setPrefix(player.getPrefix());
        entity.setStaffChat(player.isStaffChat());
        entity.setIps(GSON.toJson(player.getIps()));
        entity.setCoins(player.getCoins());
        entity.setVanished(player.isVanished());
        entity.setCommandSpy(player.isCommandSpy());
        return entity;
    }

    private List<String> parseIps(String ips)
    {
        if (ips == null || ips.isBlank())
        {
            return List.of();
        }
        List<String> parsed = GSON.fromJson(ips, new TypeToken<List<String>>()
        {
        }.getType());
        return parsed == null ? List.of() : parsed;
    }

    private void syncIps(String playerUuid, List<String> ips) throws SQLException
    {
        DeleteBuilder<PlayerIpEntity, Long> delete = playerIps.deleteBuilder();
        delete.where().eq("player_uuid", playerUuid);
        delete.delete();

        for (String ip : ips.stream().distinct().toList())
        {
            playerIps.create(new PlayerIpEntity(playerUuid, ip));
        }
    }
}
