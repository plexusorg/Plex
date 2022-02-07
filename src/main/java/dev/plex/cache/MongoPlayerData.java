package dev.plex.cache;

import dev.morphia.Datastore;
import dev.morphia.query.Query;
import dev.morphia.query.Update;
import dev.morphia.query.experimental.filters.Filters;
import dev.morphia.query.experimental.updates.UpdateOperators;
import dev.plex.Plex;
import dev.plex.player.PlexPlayer;
import java.util.UUID;

/**
 * Mongo fetching utilities for players
 */
public class MongoPlayerData
{
    /**
     * The datastore object / database
     */
    private final Datastore datastore;

    /**
     * Creates an instance of the player data
     */
    public MongoPlayerData()
    {
        this.datastore = Plex.get().getMongoConnection().getDatastore();
    }

    /**
     * Checks whether the player exists in mongo's database
     * @param uuid The unique ID of the player
     * @return true if the player was found
     */
    public boolean exists(UUID uuid)
    {
        Query<PlexPlayer> query = datastore.find(PlexPlayer.class)
                .filter(Filters.eq("uuid", uuid.toString()));

        return query.first() != null;
    }

    /**
     * Gets the player from cache or from mongo's database
     * @param uuid The unique ID of the player
     * @return a PlexPlayer object
     * @see PlexPlayer
     */
    public PlexPlayer getByUUID(UUID uuid)
    {
        if (PlayerCache.getPlexPlayerMap().containsKey(uuid))
        {
            return PlayerCache.getPlexPlayerMap().get(uuid);
        }

        Query<PlexPlayer> query2 = datastore.find(PlexPlayer.class).filter(Filters.eq("uuid", uuid.toString()));
        return query2.first();
    }

    /**
     * Updates a player's information in the mongo database
     * @param player The PlexPlayer object
     * @see PlexPlayer
     */
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
                        UpdateOperators.set("vanished", player.isVanished()),
                        UpdateOperators.set("commandspy", player.isCommandSpy()));

        updateOps.execute();
    }


    /**
     * Saves the player's information in the database
     * @param plexPlayer The PlexPlayer object
     * @see PlexPlayer
     */
    public void save(PlexPlayer plexPlayer)
    {
        datastore.save(plexPlayer);
    }
}