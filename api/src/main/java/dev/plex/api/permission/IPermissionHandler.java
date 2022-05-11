package dev.plex.api.permission;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IPermissionHandler
{
    default boolean hasPermission(@NotNull Player player, @Nullable String permission)
    {
        if (permission == null)
        {
            return true;
        }
        return player.hasPermission(permission);
    }

    default boolean hasPermission(@NotNull OfflinePlayer player, @Nullable String permission)
    {
        if (permission == null)
        {
            return true;
        }
        if (player.isOnline() && Bukkit.getPlayer(player.getUniqueId()) != null)
        {
            return Bukkit.getPlayer(player.getUniqueId()).hasPermission(permission);
        }
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
