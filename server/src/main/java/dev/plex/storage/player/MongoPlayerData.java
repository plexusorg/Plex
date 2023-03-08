package dev.plex.storage.player;

import dev.morphia.Datastore;
import dev.morphia.query.Query;
import dev.morphia.query.Update;
import dev.morphia.query.filters.Filters;
import dev.morphia.query.updates.UpdateOperators;
import dev.plex.Plex;
import dev.plex.player.PlexPlayer;

import java.util.Collections;
import java.util.List;
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
     *
     * @param uuid The unique ID of the player
     * @return true if the player was found
     */
    public boolean exists(UUID uuid)
    {
        Query<PlexPlayer> query = datastore.find(PlexPlayer.class)
                .filter(Filters.eq("uuid", uuid));

        return query.first() != null;
    }

    public boolean exists(String username)
    {
        Query<PlexPlayer> query = datastore.find(PlexPlayer.class)
                .filter(Filters.regex("name").caseInsensitive().pattern(username));

        return query.first() != null;
    }

    /**
     * Gets the player from cache or from mongo's database
     *
     * @param uuid The unique ID of the player
     * @return a PlexPlayer object
     * @see PlexPlayer
     */
    public PlexPlayer getByUUID(UUID uuid)
    {
        if (Plex.get().getPlayerCache().getPlexPlayerMap().containsKey(uuid))
        {
            return Plex.get().getPlayerCache().getPlexPlayerMap().get(uuid);
        }

        Query<PlexPlayer> query2 = datastore.find(PlexPlayer.class).filter(Filters.eq("uuid", uuid));
        return query2.first();
    }

    public PlexPlayer getByName(String username)
    {
        PlexPlayer player = Plex.get().getPlayerCache().getPlexPlayerMap().values().stream().filter(plexPlayer -> plexPlayer.getName().equalsIgnoreCase(username)).findFirst().orElse(null);
        if (player != null)
        {
            return player;
        }

        Query<PlexPlayer> query2 = datastore.find(PlexPlayer.class).filter(Filters.regex("name").caseInsensitive().pattern(username));
        return query2.first();
    }

    /**
     * Gets the player from cache or from mongo's database
     *
     * @param ip The IP address of the player.
     * @return a PlexPlayer object
     * @see PlexPlayer
     */
    public PlexPlayer getByIP(String ip)
    {
        PlexPlayer player = Plex.get().getPlayerCache().getPlexPlayerMap().values().stream().filter(plexPlayer -> plexPlayer.getIps().contains(ip)).findFirst().orElse(null);
        if (player != null)
        {
            return player;
        }

        Query<PlexPlayer> query2 = datastore.find(PlexPlayer.class).filter(Filters.in("ips", Collections.singleton(ip)));
        return query2.first();
    }

    /**
     * Updates a player's information in the mongo database
     *
     * @param player The PlexPlayer object
     * @see PlexPlayer
     */
    public void update(PlexPlayer player)
    {
        Query<PlexPlayer> filter = datastore.find(PlexPlayer.class)
                .filter(Filters.eq("uuid", player.getUuid()));

        Update<PlexPlayer> updateOps = filter.update(
                UpdateOperators.set("name", player.getName()),
                UpdateOperators.set("loginMessage", player.getLoginMessage()),
                UpdateOperators.set("prefix", player.getPrefix()),
                UpdateOperators.set("vanished", player.isVanished()),
                UpdateOperators.set("commandSpy", player.isCommandSpy()),
                UpdateOperators.set("adminActive", player.isAdminActive()),
                UpdateOperators.set("rank", player.getRank().toLowerCase()),
                UpdateOperators.set("ips", player.getIps()),
                UpdateOperators.set("coins", player.getCoins()),
                UpdateOperators.set("punishments", player.getPunishments()),
                UpdateOperators.set("notes", player.getNotes()));

        updateOps.execute();
    }

    public List<PlexPlayer> getPlayers()
    {
        return datastore.find(PlexPlayer.class).stream().toList();
    }


    /**
     * Saves the player's information in the database
     *
     * @param plexPlayer The PlexPlayer object
     * @see PlexPlayer
     */
    public void save(PlexPlayer plexPlayer)
    {
        datastore.save(plexPlayer);
    }
}