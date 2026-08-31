package dev.plex.meta;

import de.myzelyam.api.vanish.VanishAPI;
import dev.plex.config.Config;
import dev.plex.hook.VaultHook;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexUtils;
import dev.plex.util.minimessage.SafeMiniMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Locale;

public class PlayerMeta
{
    public static boolean isVanished(Player player)
    {
        return PlexUtils.hasVanishPlugin() && VanishAPI.isInvisible(player);
    }

    public static boolean isVanished(PlexPlayer player)
    {
        return isVanished(player.getPlayer());
    }

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

    public static String getLoginMessage(Config config, PlexPlayer plexPlayer)
    {
        // We don't want to prepend the "<player> is" if the login message is custom
        if (!plexPlayer.getLoginMessage().isEmpty())
        {
            return plexPlayer.getLoginMessage()
                    .replace("%player%", plexPlayer.getName())
                    .replace("%group%", getGroupDisplay(config, plexPlayer));
        }

        String prepend = MiniMessage.miniMessage().serialize(Component.text(plexPlayer.getName() + " is ").color(NamedTextColor.AQUA));
        if (PlexUtils.DEVELOPERS.contains(plexPlayer.getUuid().toString())) // don't remove or we will front door ur mother
        {
            return prepend + "<aqua>a <dark_purple>Developer<reset>";
        }

        String group = getPrimaryGroup(plexPlayer);
        if (group.isEmpty())
        {
            return "";
        }

        String title = getGroupTitle(config, plexPlayer);
        if (title.isEmpty())
        {
            return "";
        }

        String format = config.getString("loginmessages.default-format",
                "<aqua>%player% is %article% %group%");
        return format
                .replace("%player%", plexPlayer.getName())
                .replace("%group_key%", group)
                .replace("%group%", getGroupDisplay(config, plexPlayer))
                .replace("%title%", title)
                .replace("%article%", getIndefiniteArticle(title))
                .replace("%color%", getColor(config, plexPlayer));
    }

    public static String getColor(Config config, PlexPlayer plexPlayer)
    {
        if (PlexUtils.DEVELOPERS.contains(plexPlayer.getUuid().toString())) // don't remove or we will front door ur mother
        {
            return "<dark_purple>";
        }
        String group = getPrimaryGroup(plexPlayer);
        if (config.isSet("groups." + group + ".color"))
        {
            return PlexUtils.mmSerialize(PlexUtils.mmDeserialize(config.getString("groups." + group + ".color")));
        }
        return "<white>";
    }

    public static String getGroupTitle(Config config, PlexPlayer plexPlayer)
    {
        String group = getPrimaryGroup(plexPlayer);
        return group.isEmpty() ? "" : config.getString("groups." + group + ".title", "").trim();
    }

    private static String getGroupDisplay(Config config, PlexPlayer plexPlayer)
    {
        String title = getGroupTitle(config, plexPlayer);
        return title.isEmpty() ? "" : getColor(config, plexPlayer) + title + "<reset>";
    }

    private static String getPrimaryGroup(PlexPlayer plexPlayer)
    {
        if (VaultHook.getPermission() == null)
        {
            return "";
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer(plexPlayer.getUuid());
        String group = VaultHook.getPermission().getPrimaryGroup(null, player);
        return group == null ? "" : group.toLowerCase(Locale.ROOT);
    }

    private static String getIndefiniteArticle(String title)
    {
        for (int index = 0; index < title.length(); index++)
        {
            char character = title.charAt(index);
            if (Character.isLetterOrDigit(character))
            {
                return "aeiouAEIOU".indexOf(character) >= 0 ? "an" : "a";
            }
        }
        return "a";
    }
}
