package dev.plex.hook;

import dev.plex.Plex;
import dev.plex.meta.PlayerMeta;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class VaultHook
{
    private static Chat CHAT;
    private static Permission PERMISSIONS;

    static
    {
        CHAT = setupChat();
        PERMISSIONS = setupPermissions();
    }

    private static Chat setupChat()
    {
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault"))
        {
            return null;
        }
        RegisteredServiceProvider<Chat> rsp = Bukkit.getServicesManager().getRegistration(Chat.class);
        if (rsp == null)
        {
            return null;
        }
        CHAT = rsp.getProvider();
        return CHAT;
    }

    private static Permission setupPermissions()
    {
        if (!Bukkit.getPluginManager().isPluginEnabled("Vault"))
        {
            return null;
        }
        RegisteredServiceProvider<Permission> rsp = Bukkit.getServicesManager().getRegistration(Permission.class);
        if (rsp == null)
        {
            return null;
        }
        PERMISSIONS = rsp.getProvider();
        return PERMISSIONS;
    }

    public static Component getPrefix(PlexPlayer plexPlayer)
    {
        if (VaultHook.getChat() == null || VaultHook.getPermission() == null)
        {
            return null;
        }
        if (PlexUtils.DEVELOPERS.contains(plexPlayer.getUuid().toString()))
        {
            return PlexUtils.mmDeserialize("<dark_gray>[<dark_purple>Developer<dark_gray>]");
        }
        Player bukkitPlayer = Bukkit.getPlayer(plexPlayer.getUuid());
        String group = VaultHook.getPermission().getPrimaryGroup(bukkitPlayer);
        String vaultPrefix = VaultHook.getChat().getGroupPrefix(bukkitPlayer.getWorld(), group);
        return LegacyComponentSerializer.legacyAmpersand().deserialize(vaultPrefix);
    }

    public static Permission getPermission()
    {
        return PERMISSIONS;
    }

    public static Chat getChat()
    {
        return CHAT;
    }
}
