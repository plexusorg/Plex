package dev.plex.cache;

import dev.plex.Plex;
import dev.plex.player.PlexPlayer;
import dev.plex.storage.StorageType;
import org.bukkit.Bukkit;

import java.util.UUID;

public class DataUtils
{
                /* PLEX PLAYER METHODS */

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

    public static PlexPlayer getPlayer(UUID uuid)
    {
        if (PlayerCache.getPlexPlayerMap().containsKey(uuid))
        {
            return PlayerCache.getPlexPlayerMap().get(uuid);
        }

        if (Plex.get().getStorageType() == StorageType.MONGODB)
        {
            return Plex.get().getMongoPlayerData().getByUUID(uuid);
        }
        else
        {
            return Plex.get().getSqlPlayerData().getByUUID(uuid);
        }
    }

    public static PlexPlayer getPlayer(String name)
    {
        return getPlayer(Bukkit.getPlayer(name).getUniqueId());
    }

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

    public static void insert(PlexPlayer plexPlayer)
    {
        if (Plex.get().getStorageType() == StorageType.MONGODB)
        {
            Plex.get().getMongoPlayerData().save(plexPlayer);
        } else {
            Plex.get().getSqlPlayerData().insert(plexPlayer);
        }
    }

    /*           REDIS METHODS AT ONE POINT FOR BANS, AND JSON METHODS FOR PUNISHMENTS       */

}
