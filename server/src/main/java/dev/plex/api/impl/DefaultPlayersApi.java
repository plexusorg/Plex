package dev.plex.api.impl;

import dev.plex.Plex;
import dev.plex.api.player.PlayersApi;
import dev.plex.api.player.PlexPlayerView;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexUtils;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class DefaultPlayersApi implements PlayersApi
{
    private final Plex plugin;

    DefaultPlayersApi(Plex plugin) { this.plugin = plugin; }

    @Override public Optional<? extends PlexPlayerView> byUuid(UUID uuid) { return Optional.ofNullable(plugin.getPlayerService().getPlayer(uuid)).map(DefaultPlexPlayerView::new); }
    @Override public Optional<? extends PlexPlayerView> byName(String name) { return Optional.ofNullable(plugin.getPlayerService().getPlayer(name)).map(DefaultPlexPlayerView::new); }
    @Override public List<String> onlineNames() { return PlexUtils.getPlayerNameList(); }

    static PlexPlayer unwrap(PlexPlayerView view)
    {
        if (view instanceof DefaultPlexPlayerView wrapped) return wrapped.player();
        return null;
    }
}
