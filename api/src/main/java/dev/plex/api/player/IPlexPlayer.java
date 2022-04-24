package dev.plex.api.player;

import java.util.List;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;

public interface IPlexPlayer
{
    String getId();

    UUID getUuid();

    String getName();

    Player getPlayer();

    void setPlayer(Player player);

    String getLoginMessage();

    void setLoginMessage(String message);

    String getPrefix();

    void setPrefix(String prefix);

    boolean isVanished();

    void setVanished(boolean vanished);

    boolean isCommandSpy();

    void setCommandSpy(boolean commandSpy);

    boolean isFrozen();

    void setFrozen(boolean frozen);

    boolean isMuted();

    void setMuted(boolean muted);

    boolean isLockedUp();

    void setLockedUp(boolean lockedUp);

    boolean isAdminActive();

    void setAdminActive(boolean active);

    long getCoins();

    void setCoins(long coins);

    String getRank();

    void setRank(String rank);

    List<String> getIps();

    void setIps(List<String> ips);

    PermissionAttachment getPermissionAttachment();

    void setPermissionAttachment(PermissionAttachment attachment);
}
