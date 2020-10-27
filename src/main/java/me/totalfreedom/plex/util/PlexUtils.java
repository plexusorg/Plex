package me.totalfreedom.plex.util;

import me.totalfreedom.plex.Plex;
import me.totalfreedom.plex.storage.StorageType;

import java.sql.SQLException;

public class PlexUtils
{

    public static void testConnections()
    {
        if (Plex.get().getSqlConnection().getCon() != null)
        {
            if (Plex.get().getStorageType() == StorageType.SQL)
            {
                PlexLog.log("Successfully enabled MySQL!");
            } else if (Plex.get().getStorageType() == StorageType.SQLITE)
            {
                PlexLog.log("Successfully enabled SQLite!");
            }
            try {
                Plex.get().getSqlConnection().getCon().close();
            } catch (SQLException throwables) {
            }
        } else if (Plex.get().getMongoConnection() != null)
        {
            PlexLog.log("Successfully enabled MongoDB!");
        }
    }

}
