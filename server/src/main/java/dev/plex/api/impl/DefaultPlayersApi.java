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
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

final class DefaultPlayersApi implements PlayersApi
{
    private final Plex plugin;

    DefaultPlayersApi(Plex plugin) { this.plugin = plugin; }

    @Override public CompletableFuture<Optional<PlexPlayerView>> player(UUID uuid) { return plugin.getPlayerService().findPlayer(Objects.requireNonNull(uuid, "uuid")).thenApply(player -> Optional.ofNullable(player).map(DefaultPlexPlayerView::new)); }
    @Override public CompletableFuture<Optional<PlexPlayerView>> byName(String name) { return plugin.getPlayerService().findPlayer(Objects.requireNonNull(name, "name")).thenApply(player -> Optional.ofNullable(player).map(DefaultPlexPlayerView::new)); }
    @Override public List<String> onlineNames() { return List.copyOf(PlexUtils.getPlayerNameList()); }
    @Override public PlayerModuleData moduleData(PlexModule module, UUID playerUuid) { return new DefaultPlayerModuleData(plugin.getPlayerModuleDataRepository(), plugin.getDatabaseExecutor(), ModuleNames.prefix(Objects.requireNonNull(module, "module")), Objects.requireNonNull(playerUuid, "playerUuid")); }

    static PlexPlayer unwrap(PlexPlayerView view)
    {
        if (view instanceof DefaultPlexPlayerView wrapped) return wrapped.player();
        return null;
    }
}
