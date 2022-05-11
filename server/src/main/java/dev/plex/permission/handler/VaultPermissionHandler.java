package dev.plex.permission.handler;

import dev.plex.api.permission.IPermissionHandler;
import dev.plex.hook.VaultHook;
import lombok.Getter;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@Getter
public class VaultPermissionHandler implements IPermissionHandler
{
    @Override
    public boolean hasPermission(@NotNull OfflinePlayer player, @Nullable String permission)
    {
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault"))
        {
            return IPermissionHandler.super.hasPermission(player, permission);
        }
        return VaultHook.getPermission().playerHas(null, player, permission);

    }
}
