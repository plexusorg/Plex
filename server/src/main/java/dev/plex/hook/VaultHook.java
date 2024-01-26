package dev.plex.hook;

import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexLog;
import dev.plex.util.PlexUtils;
import dev.plex.util.minimessage.SafeMiniMessage;
import net.kyori.adventure.text.Component;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.UUID;

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

    public static Component getPrefix(UUID uuid)
    {
        return getPrefix(Bukkit.getOfflinePlayer(uuid));
    }

    public static Component getPrefix(PlexPlayer plexPlayer)
    {
        return getPrefix(Bukkit.getOfflinePlayer(plexPlayer.getUuid()));
    }

    public static Component getPrefix(OfflinePlayer player)
    {
        if (VaultHook.getChat() == null || VaultHook.getPermission() == null)
        {
            return Component.empty();
        }
        if (PlexUtils.DEVELOPERS.contains(player.getUniqueId().toString()))
        {
            return PlexUtils.mmDeserialize("<dark_gray>[<dark_purple>Developer<dark_gray>]");
        }
        String group = VaultHook.getPermission().getPrimaryGroup(null, player);
        if (group == null || group.isEmpty()) {
            return Component.empty();
        }
        String vaultPrefix = VaultHook.getChat().getGroupPrefix((String) null, group);
        if (vaultPrefix == null || vaultPrefix.isEmpty()) {
            return Component.empty();
        }
        PlexLog.debug("prefix: {0}", SafeMiniMessage.mmSerializeWithoutEvents(PlexUtils.stringToComponent(vaultPrefix)).replace("<", "\\<"));
        return PlexUtils.stringToComponent(vaultPrefix);
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
