package dev.plex.api.impl;

import dev.plex.Plex;
import dev.plex.api.player.PlayerModuleData;
import dev.plex.api.player.PlayersApi;
import dev.plex.api.player.PlexPlayerView;
import dev.plex.module.PlexModule;
import dev.plex.player.PlexPlayer;
import dev.plex.storage.module.ModuleNames;
import dev.plex.util.PlexUtils;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class DefaultPlayersApi implements PlayersApi
{
    private final Plex plugin;

    DefaultPlayersApi(Plex plugin) { this.plugin = plugin; }

    @Override public Optional<? extends PlexPlayerView> player(UUID uuid) { return Optional.ofNullable(plugin.getPlayerService().getPlayer(uuid)).map(player -> new DefaultPlexPlayerView(player, plugin.getPlayerNameResolver())); }
    @Override public Optional<? extends PlexPlayerView> byName(String name) { return Optional.ofNullable(plugin.getPlayerService().getPlayer(name)).map(player -> new DefaultPlexPlayerView(player, plugin.getPlayerNameResolver())); }
    @Override public List<String> onlineNames() { return PlexUtils.getPlayerNameList(); }
    @Override public PlayerModuleData moduleData(PlexModule module, UUID playerUuid) { return new DefaultPlayerModuleData(plugin.getPlayerModuleDataRepository(), ModuleNames.prefix(module), playerUuid); }

    static PlexPlayer unwrap(PlexPlayerView view)
    {
        if (view instanceof DefaultPlexPlayerView wrapped) return wrapped.player();
        return null;
    }
}
