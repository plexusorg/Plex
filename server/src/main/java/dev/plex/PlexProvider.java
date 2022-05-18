package dev.plex;

import dev.plex.api.IPlayerCache;
import dev.plex.api.PlexApi;
import dev.plex.player.PlexPlayer;

public class PlexProvider implements PlexApi
{
    @Override
    public IPlayerCache<PlexPlayer> getPlayerCache()
    {
        return Plex.get().getPlayerCache();
    }
}
