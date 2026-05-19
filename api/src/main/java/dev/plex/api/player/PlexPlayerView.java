package dev.plex.api.player;

import java.util.List;
import java.util.UUID;
import dev.plex.api.punishment.PunishmentView;
import org.bukkit.entity.Player;

public interface PlexPlayerView
{
    UUID uuid();
    String name();
    List<String> ips();
    List<? extends PunishmentView> punishments();
    boolean frozen();
    boolean muted();
    boolean lockedUp();
    Player bukkitPlayer();
}
