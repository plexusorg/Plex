package dev.plex.api.impl;

import dev.plex.api.punishment.IndefiniteBanView;
import dev.plex.punishment.PunishmentManager;
import java.util.List;
import java.util.UUID;

record DefaultIndefiniteBanView(PunishmentManager.IndefiniteBan ban) implements IndefiniteBanView
{
    @Override public List<String> usernames() { return List.copyOf(ban.getUsernames()); }
    @Override public List<UUID> uuids() { return List.copyOf(ban.getUuids()); }
    @Override public List<String> ips() { return List.copyOf(ban.getIps()); }
    @Override public String reason() { return ban.getReason(); }
}
