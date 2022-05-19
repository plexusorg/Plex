package dev.plex.hook;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
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

    public static Permission getPermission()
    {
        return PERMISSIONS;
    }

    public static Chat getChat()
    {
        return CHAT;
    }
}
