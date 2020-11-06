package me.totalfreedom.plex.cache;

import dev.morphia.Datastore;
import dev.morphia.query.Query;
import dev.morphia.query.Update;
import dev.morphia.query.experimental.filters.Filters;
import dev.morphia.query.experimental.updates.UpdateOperators;
import java.util.UUID;
import me.totalfreedom.plex.Plex;
import me.totalfreedom.plex.player.PlexPlayer;

public class MongoPlayerData
{
    private final Datastore datastore;

    public MongoPlayerData()
    {
        this.datastore = Plex.get().getMongoConnection().getDatastore();
    }

    public boolean exists(UUID uuid)
    {
        Query<PlexPlayer> query = datastore.find(PlexPlayer.class)
                .filter(Filters.eq("uuid", uuid.toString()));

        return query.first() != null;
    }

    public PlexPlayer getByUUID(UUID uuid)
    {
        if (PlayerCache.getPlexPlayerMap().containsKey(uuid))
        {
            return PlayerCache.getPlexPlayerMap().get(uuid);
        }

        Query<PlexPlayer> query2 = datastore.find(PlexPlayer.class).filter(Filters.eq("uuid", uuid.toString()));
        return query2.first();
    }

    public void update(PlexPlayer player)
    {
        Query<PlexPlayer> filter = datastore.find(PlexPlayer.class)
                .filter(Filters.eq("uuid", player.getUuid()));

        Update<PlexPlayer> updateOps = filter
                .update(
                        UpdateOperators.set("name", player.getName()),
                        UpdateOperators.set("loginMSG", player.getLoginMSG()),
                        UpdateOperators.set("prefix", player.getPrefix()),
                        UpdateOperators.set("rank", player.getRank().toLowerCase()),
                        UpdateOperators.set("ips", player.getIps()),
                        UpdateOperators.set("coins", player.getCoins()),
                        UpdateOperators.set("vanished", player.isVanished()));

        updateOps.execute();
    }


    public void save(PlexPlayer plexPlayer)
    {
        datastore.save(plexPlayer);
    }

}