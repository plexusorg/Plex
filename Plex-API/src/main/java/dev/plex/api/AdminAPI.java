package dev.plex.api;

import dev.plex.PlexBase;
import dev.plex.player.PlexPlayer;
import java.util.List;

public class AdminAPI extends PlexBase
{
    /**
     * Gathers every admins username (cached and in the database)
     *
     * @return An array list of the names of every admin
     */
    public List<String> getAllAdmins()
    {
        return plugin.getAdminList().getAllAdmins();
    }

    public List<PlexPlayer> getAllAdminPlayers()
    {
        return plugin.getAdminList().getAllAdminPlayers();
    }
}
