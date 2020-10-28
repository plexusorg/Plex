package me.totalfreedom.plex.cache;

import dev.morphia.Datastore;
import dev.morphia.query.Query;
import dev.morphia.query.UpdateOperations;
import me.totalfreedom.plex.Plex;
import me.totalfreedom.plex.player.PlexPlayer;

import java.util.UUID;

public class MongoPlayerData
{
    private final Datastore datastore;

    public MongoPlayerData()
    {
        this.datastore = Plex.get().getMongoConnection().getDatastore();
    }

    public boolean exists(UUID uuid)
    {
        Query<PlexPlayer> query = datastore.createQuery(PlexPlayer.class);

        return query.field("uuid").exists().field("uuid").equal(uuid.toString()).first() != null;
    }

    public PlexPlayer getByUUID(UUID uuid)
    {

        if (PlayerCache.getPlexPlayerMap().containsKey(uuid))
        {
            return PlayerCache.getPlexPlayerMap().get(uuid);
        }
        Query<PlexPlayer> query2 = datastore.createQuery(PlexPlayer.class).field("uuid").exists().field("uuid").equal(uuid.toString());
        return query2.first();
    }

    public void update(PlexPlayer player)
    {
        Query<PlexPlayer> filter = datastore.createQuery(PlexPlayer.class)
                .field("uuid").equal(player.getUuid());

        UpdateOperations<PlexPlayer> updateOps = datastore.createUpdateOperations(PlexPlayer.class);

        updateOps.set("name", player.getName());
        updateOps.set("loginMSG", player.getLoginMSG());
        updateOps.set("prefix", player.getPrefix());
        updateOps.set("rank", player.getRank().toLowerCase());
        updateOps.set("ips", player.getIps());
        updateOps.set("coins", player.getCoins());
        datastore.update(filter, updateOps);
    }

    public void save(PlexPlayer plexPlayer)
    {
        datastore.save(plexPlayer);
    }

}