package me.totalfreedom.plex.cache;

import java.util.UUID;
import me.totalfreedom.plex.Plex;
import me.totalfreedom.plex.player.PlexPlayer;
import me.totalfreedom.plex.storage.StorageType;

public class DataUtils
{

    public static boolean hasPlayedBefore(UUID uuid)
    {
        if (Plex.get().getStorageType() == StorageType.MONGO)
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

        if (Plex.get().getStorageType() == StorageType.MONGO)
        {
            return Plex.get().getMongoPlayerData().getByUUID(uuid);
        }
        else
        {
            return Plex.get().getSqlPlayerData().getByUUID(uuid);
        }
    }

    public static void update(PlexPlayer plexPlayer)
    {
        if (Plex.get().getStorageType() == StorageType.MONGO)
        {
            Plex.get().getMongoPlayerData().update(plexPlayer);
        }
        else
        {
            Plex.get().getSqlPlayerData().update(plexPlayer);
        }
    }

}
