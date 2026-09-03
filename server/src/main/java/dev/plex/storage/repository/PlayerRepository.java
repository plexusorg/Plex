package dev.plex.storage.repository;

import dev.plex.player.PlexPlayer;

import java.util.UUID;

public interface PlayerRepository
{
    boolean exists(UUID uuid);

    boolean exists(String username);

    PlexPlayer getByUUID(UUID uuid, boolean loadExtraData);

    String getNameByUUID(UUID uuid);

    PlexPlayer getByName(String username, boolean loadExtraData);

    PlexPlayer getByIP(String ip);

    void update(PlexPlayer player);

    void insert(PlexPlayer player);
}
