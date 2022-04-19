package dev.plex.util;

import dev.plex.Plex;
import dev.plex.cache.DataUtils;
import dev.plex.permission.Permission;
import dev.plex.player.PlexPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.jetbrains.annotations.NotNull;

public class PermissionsUtil
{
    public static void setupPermissions(@NotNull Player player)
    {
        PlexPlayer plexPlayer = DataUtils.getPlayer(player.getUniqueId());
        PermissionAttachment attachment = player.addAttachment(Plex.get());
        plexPlayer.getPermissions().forEach(permission -> attachment.setPermission(permission.getPermission(), permission.isAllowed()));
        plexPlayer.setPermissionAttachment(attachment);
    }

    public static void addPermission(PlexPlayer player, Permission permission)
    {
        Plex.get().getSqlPermissions().addPermission(PlexUtils.addToArrayList(player.getPermissions(), permission));
        Player p = Bukkit.getPlayer(player.getUuid());
        if (p == null)
        {
            return;
        }
        player.getPermissionAttachment().setPermission(permission.getPermission(), permission.isAllowed());
    }

    public static void addPermission(PlexPlayer player, String permission)
    {
        addPermission(player, new Permission(player.getUuid(), permission));
    }

    public static void removePermission(PlexPlayer player, String permission)
    {
        Plex.get().getSqlPermissions().removePermission(player.getUuid(), permission);
        player.getPermissions().removeIf(permission1 -> permission1.getPermission().equalsIgnoreCase(permission));
        Player p = Bukkit.getPlayer(player.getUuid());
        if (p == null)
        {
            return;
        }
        player.getPermissionAttachment().unsetPermission(permission);
    }

    public static void updatePermission(PlexPlayer player, String permission, boolean newValue)
    {
        player.getPermissions().stream().filter(permission1 -> permission.equalsIgnoreCase(permission)).findFirst().ifPresent(permission1 ->
        {
            Plex.get().getSqlPermissions().updatePermission(permission1, newValue);
        });
        player.getPermissions().removeIf(permission1 -> permission1.getPermission().equalsIgnoreCase(permission));
        Player p = Bukkit.getPlayer(player.getUuid());
        if (p == null)
        {
            return;
        }
        player.getPermissionAttachment().unsetPermission(permission);

    }
}
