package dev.plex.player;

import dev.plex.cache.PlayerCache;
import dev.plex.storage.repository.PlayerRepository;

import java.util.Optional;
import java.util.UUID;

public class PlayerService
{
    private final PlayerCache playerCache;
    private final PlayerRepository playerRepository;

    public PlayerService(PlayerCache playerCache, PlayerRepository playerRepository)
    {
        this.playerCache = playerCache;
        this.playerRepository = playerRepository;
    }

    public boolean hasPlayedBefore(UUID uuid)
    {
        return playerRepository.exists(uuid);
    }

    public boolean hasPlayedBefore(String username)
    {
        return playerRepository.exists(username);
    }

    public PlexPlayer getPlayer(UUID uuid)
    {
        return getPlayer(uuid, true);
    }

    public PlexPlayer getPlayer(UUID uuid, boolean loadExtraData)
    {
        if (playerCache.getPlexPlayerMap().containsKey(uuid))
        {
            return playerCache.getPlexPlayerMap().get(uuid);
        }

        return playerRepository.getByUUID(uuid, loadExtraData);
    }

    public PlexPlayer getPlayer(String username)
    {
        return getPlayer(username, true);
    }

    public PlexPlayer getPlayer(String username, boolean loadExtraData)
    {
        Optional<PlexPlayer> plexPlayer = playerCache.getPlexPlayerMap().values().stream().filter(player -> player.getName().equalsIgnoreCase(username)).findFirst();
        return plexPlayer.orElseGet(() -> playerRepository.getByName(username, loadExtraData));
    }

    public PlexPlayer getPlayerByIP(String ip)
    {
        PlexPlayer player = playerCache.getPlexPlayerMap().values().stream().filter(plexPlayer -> plexPlayer.getIps().contains(ip)).findFirst().orElse(null);
        if (player != null)
        {
            return player;
        }

        return playerRepository.getByIP(ip);
    }

    public String getNameByUUID(UUID uuid)
    {
        PlexPlayer player = playerCache.getPlexPlayer(uuid);
        if (player != null)
        {
            return player.getName();
        }
        return playerRepository.getNameByUUID(uuid);
    }

    public void update(PlexPlayer plexPlayer)
    {
        playerRepository.update(plexPlayer);
    }

    public void insert(PlexPlayer plexPlayer)
    {
        playerRepository.insert(plexPlayer);
    }
}
