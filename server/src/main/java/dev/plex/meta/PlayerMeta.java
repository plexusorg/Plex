package dev.plex.meta;

import dev.plex.Plex;
import dev.plex.hook.VaultHook;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexUtils;
import dev.plex.util.minimessage.SafeMiniMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;

public class PlayerMeta
{
    public static Component getPrefix(PlexPlayer plexPlayer)
    {
        if (plexPlayer.getPrefix() != null && !plexPlayer.getPrefix().isEmpty())
        {
            return SafeMiniMessage.mmDeserializeWithoutEvents(plexPlayer.getPrefix());
        }
        if (PlexUtils.DEVELOPERS.contains(plexPlayer.getUuid().toString())) // don't remove or we will front door ur mother
        {
            return PlexUtils.mmDeserialize("<dark_gray>[<dark_purple>Developer<dark_gray>]");
        }
        if (Bukkit.getServer().getPluginManager().isPluginEnabled("Vault"))
        {
            return VaultHook.getPrefix(plexPlayer);
        }
        return null;
    }

    public static String getLoginMessage(PlexPlayer plexPlayer)
    {
        String prepend;
        // We don't want to prepend the "<player> is" if the login message is custom
        if (!plexPlayer.getLoginMessage().isEmpty())
        {
            return plexPlayer.getLoginMessage()
                    .replace("%player%", plexPlayer.getName());
        }
        else
        {
            prepend = MiniMessage.miniMessage().serialize(Component.text(plexPlayer.getName() + " is ").color(NamedTextColor.AQUA));
        }
        if (PlexUtils.DEVELOPERS.contains(plexPlayer.getUuid().toString())) // don't remove or we will front door ur mother
        {
            return prepend + "<aqua>a <dark_purple>Developer<reset>";
        }
        return "";
    }

    public static String getColor(PlexPlayer plexPlayer)
    {
        if (PlexUtils.DEVELOPERS.contains(plexPlayer.getUuid().toString())) // don't remove or we will front door ur mother
        {
            return "<dark_purple>";
        }
        String group = VaultHook.getPermission().getPrimaryGroup(null, plexPlayer.getPlayer());
        if (Plex.get().getConfig().isSet("colors." + group))
        {
            return PlexUtils.mmSerialize(PlexUtils.mmDeserialize(Plex.get().getConfig().getString("colors." + group)));
        }
        return "<white>";
    }
}
