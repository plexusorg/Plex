package dev.plex.api.impl;

import dev.plex.Plex;
import dev.plex.api.player.PlexPlayerView;
import dev.plex.api.punishment.PunishmentView;
import dev.plex.api.punishment.PunishmentType;
import dev.plex.player.PlexPlayer;
import java.util.List;
import java.util.UUID;

record DefaultPlexPlayerView(Plex plugin, PlexPlayer player) implements PlexPlayerView
{
    @Override public UUID uuid() { return player.getUuid(); }
    @Override public String name() { return player.getName(); }
    @Override public List<String> ips() { return List.copyOf(player.getIps()); }
    @Override public List<PunishmentView> punishments() { return player.getPunishments().stream().<PunishmentView>map(punishment ->
            new DefaultPunishmentView(punishment, plugin.getPunishmentManager().isPunishmentActive(punishment))).toList(); }
    @Override public boolean frozen() { return plugin.getPunishmentManager().hasActivePunishment(player, PunishmentType.FREEZE); }
    @Override public boolean muted() { return plugin.getPunishmentManager().hasActivePunishment(player, PunishmentType.MUTE); }
    @Override public boolean lockedUp() { return player.isLockedUp(); }
    @Override public boolean staffChat() { return player.isStaffChat(); }
}
