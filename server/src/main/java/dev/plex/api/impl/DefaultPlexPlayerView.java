package dev.plex.api.impl;

import dev.plex.api.player.PlexPlayerView;
import dev.plex.api.punishment.PunishmentView;
import dev.plex.player.PlayerNameResolver;
import dev.plex.player.PlexPlayer;
import java.util.List;
import java.util.UUID;
import org.bukkit.entity.Player;

record DefaultPlexPlayerView(PlexPlayer player, PlayerNameResolver playerNameResolver) implements PlexPlayerView
{
    @Override public UUID uuid() { return player.getUuid(); }
    @Override public String name() { return player.getName(); }
    @Override public List<String> ips() { return List.copyOf(player.getIps()); }
    @Override public List<? extends PunishmentView> punishments() { return player.getPunishments().stream().map(punishment -> new DefaultPunishmentView(punishment, playerNameResolver)).toList(); }
    @Override public boolean frozen() { return player.isFrozen(); }
    @Override public boolean muted() { return player.isMuted(); }
    @Override public boolean lockedUp() { return player.isLockedUp(); }
    @Override public boolean staffChat() { return player.isStaffChat(); }
    @Override public Player bukkitPlayer() { return player.getPlayer(); }
}
