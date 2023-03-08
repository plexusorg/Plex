package dev.plex.cache;

import dev.plex.Plex;
import dev.plex.player.PlexPlayer;
import dev.plex.storage.StorageType;

import java.util.Optional;
import java.util.UUID;

/**
 * Parent cache class
 */
public class DataUtils
{
    /**
     * Checks if the player has been on the server before
     *
     * @param uuid The unique ID of the player
     * @return true if the player is registered in the database
     */
    public static boolean hasPlayedBefore(UUID uuid)
    {
        if (Plex.get().getStorageType() == StorageType.MONGODB)
        {
            return Plex.get().getMongoPlayerData().exists(uuid);
        }
        else
        {
            return Plex.get().getSqlPlayerData().exists(uuid);
        }
    }

    public static boolean hasPlayedBefore(String username)
    {
        if (Plex.get().getStorageType() == StorageType.MONGODB)
        {
            return Plex.get().getMongoPlayerData().exists(username);
        }
        else
        {
            return Plex.get().getSqlPlayerData().exists(username);
        }
    }

    /**
     * Gets a player from cache or from the database
     *
     * @param uuid The unique ID of the player
     * @return a PlexPlayer object
     * @see PlexPlayer
     */
    public static PlexPlayer getPlayer(UUID uuid)
    {
        return getPlayer(uuid, true);
    }

    public static PlexPlayer getPlayer(UUID uuid, boolean loadExtraData)
    {
        if (Plex.get().getPlayerCache().getPlexPlayerMap().containsKey(uuid))
        {
            return Plex.get().getPlayerCache().getPlexPlayerMap().get(uuid);
        }

        if (Plex.get().getStorageType() == StorageType.MONGODB)
        {
            return Plex.get().getMongoPlayerData().getByUUID(uuid);
        }
        else
        {
            return Plex.get().getSqlPlayerData().getByUUID(uuid, loadExtraData);
        }
    }

    public static PlexPlayer getPlayer(String username)
    {
        return getPlayer(username, true);
    }

    public static PlexPlayer getPlayer(String username, boolean loadExtraData)
    {
        Optional<PlexPlayer> plexPlayer = Plex.get().getPlayerCache().getPlexPlayerMap().values().stream().filter(player -> player.getName().equalsIgnoreCase(username)).findFirst();
        if (plexPlayer.isPresent())
        {
            return plexPlayer.get();
        }

        if (Plex.get().getStorageType() == StorageType.MONGODB)
        {
            return Plex.get().getMongoPlayerData().getByName(username);
        }
        else
        {
            return Plex.get().getSqlPlayerData().getByName(username, loadExtraData);
        }
    }

    /**
     * Gets a player from cache or from the database
     *
     * @param ip The IP address of the player.
     * @return a PlexPlayer object
     * @see PlexPlayer
     */
    public static PlexPlayer getPlayerByIP(String ip)
    {
        PlexPlayer player = Plex.get().getPlayerCache().getPlexPlayerMap().values().stream().filter(plexPlayer -> plexPlayer.getIps().contains(ip)).findFirst().orElse(null);
        if (player != null)
        {
            return player;
        }

        if (Plex.get().getStorageType() == StorageType.MONGODB)
        {
            return Plex.get().getMongoPlayerData().getByIP(ip);
        }
        else
        {
            return Plex.get().getSqlPlayerData().getByIP(ip);
        }
    }

    /**
     * Updates a player's information in the database
     *
     * @param plexPlayer The PlexPlayer to update
     * @see PlexPlayer
     */
    public static void update(PlexPlayer plexPlayer)
    {
        if (Plex.get().getStorageType() == StorageType.MONGODB)
        {
            Plex.get().getMongoPlayerData().update(plexPlayer);
        }
        else
        {
            Plex.get().getSqlPlayerData().update(plexPlayer);
        }
    }

    /**
     * Inserts a player's information in the database
     *
     * @param plexPlayer The PlexPlayer to insert
     * @see PlexPlayer
     */
    public static void insert(PlexPlayer plexPlayer)
    {
        if (Plex.get().getStorageType() == StorageType.MONGODB)
        {
            Plex.get().getMongoPlayerData().save(plexPlayer);
        }
        else
        {
            Plex.get().getSqlPlayerData().insert(plexPlayer);
        }
    }

    /*           REDIS METHODS AT ONE POINT FOR BANS, AND JSON METHODS FOR PUNISHMENTS       */

}
