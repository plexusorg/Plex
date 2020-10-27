package me.totalfreedom.plex.cache;

import com.google.common.collect.Maps;
import dev.morphia.query.Query;
import dev.morphia.query.UpdateOperations;
import me.totalfreedom.plex.Plex;
import me.totalfreedom.plex.player.PlexPlayer;
import me.totalfreedom.plex.player.PunishedPlayer;

import java.util.Map;
import java.util.UUID;

public class MongoPlayerData
{

    private Map<UUID, PunishedPlayer> punishedPlayerMap = Maps.newHashMap();
    private Map<UUID, PlexPlayer> plexPlayerMap = Maps.newHashMap();

    private PlexPlayerDAO plexPlayerDAO;

    public MongoPlayerData()
    {
        this.plexPlayerDAO = new PlexPlayerDAO(PlexPlayer.class, Plex.get().getMongoConnection().getDatastore());
    }

    public boolean exists(UUID uuid) {

        Query<PlexPlayer> query = plexPlayerDAO.createQuery();

        if (query.field("uuid").exists().field("uuid").equal(uuid.toString()).find().tryNext() != null)
        {
            return true;
        }
        return false;
    }

    public PlexPlayer getByUUID(UUID uuid) {

        if (plexPlayerMap.containsKey(uuid))
        {
            return plexPlayerMap.get(uuid);
        }
        Query<PlexPlayer> query2 = plexPlayerDAO.createQuery().field("uuid").exists().field("uuid").equal(uuid.toString());
        return query2.first();
    }

    public void update(PlexPlayer player)
    {
        Query<PlexPlayer> filter = plexPlayerDAO.createQuery()
                                        .field("uuid").equal(player.getUuid());

        UpdateOperations<PlexPlayer> updateOps = plexPlayerDAO.createUpdateOperations();

        updateOps.set("name", player.getName());
        updateOps.set("loginMSG", player.getLoginMSG());
        updateOps.set("prefix", player.getPrefix());
        updateOps.set("rank", player.getRank() == null ? "" : player.getRank().name().toLowerCase());
        updateOps.set("ips", player.getIps());
        plexPlayerDAO.update(filter, updateOps);
    }


    public Map<UUID, PlexPlayer> getPlexPlayerMap() {
        return plexPlayerMap;
    }

    public Map<UUID, PunishedPlayer> getPunishedPlayerMap() {
        return punishedPlayerMap;
    }

    public PlexPlayerDAO getPlexPlayerDAO() {
        return plexPlayerDAO;
    }
}
