package dev.plex.meta;

import de.myzelyam.api.vanish.VanishAPI;
import dev.plex.config.Config;
import dev.plex.hook.VaultHook;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexUtils;
import dev.plex.util.minimessage.SafeMiniMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyFormat;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.regex.Pattern;

public class PlayerMeta
{
    private static final Pattern LEGACY_LOGIN_FORMAT = Pattern.compile("&(x(?:&[0-9a-fA-F]){6}|#[0-9a-fA-F]{6}|[0-9a-fklmnor])");

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

    public static Component getLoginMessage(Config config, PlexPlayer plexPlayer)
    {
        String format = plexPlayer.getLoginMessage();
        if (format.isEmpty() && PlexUtils.DEVELOPERS.contains(plexPlayer.getUuid().toString())) // don't remove or we will front door ur mother
        {
            return Component.text(plexPlayer.getName() + " is a ", NamedTextColor.AQUA)
                    .append(Component.text("Developer", NamedTextColor.DARK_PURPLE));
        }
        String group = getPrimaryGroup(plexPlayer);
        String title = group.isEmpty() ? "" : getGroupTitle(config, plexPlayer);
        if (format.isEmpty())
        {
            if (group.isEmpty() || title.isEmpty())
            {
                return Component.empty();
            }
            format = config.getString("loginmessages.default-format", "<aqua><player> is <article> <group>");
        }

        String color = getColor(config, plexPlayer);
        return SafeMiniMessage.mmDeserializeWithoutEvents(loginFormat(format),
                Placeholder.parsed("player", loginFormat(plexPlayer.getName())),
                Placeholder.parsed("group_key", loginFormat(group)),
                Placeholder.parsed("group", title.isEmpty() ? "" : loginFormat(color + title + "<reset>")),
                Placeholder.parsed("title", loginFormat(title)),
                Placeholder.unparsed("article", getIndefiniteArticle(title)),
                Placeholder.parsed("group_color", loginFormat(color)));
    }

    private static String loginFormat(String input)
    {
        // Convert only legacy formatting syntax; Adventure resolves every dynamic template tag.
        return LEGACY_LOGIN_FORMAT.matcher(PlexUtils.cleanString(input).replaceAll("([§&]+)(k+)", "")).replaceAll(match ->
        {
            String code = match.group(1);
            if (code.startsWith("x"))
            {
                return "<reset><#" + code.substring(1).replace("&", "") + ">";
            }
            if (code.startsWith("#"))
            {
                return "<reset><" + code + ">";
            }
            LegacyFormat format = LegacyComponentSerializer.parseChar(code.charAt(0));
            if (format.color() != null)
            {
                return "<reset><" + format.color().asHexString() + ">";
            }
            if (format.reset())
            {
                return "<reset>";
            }
            return format.decoration() == TextDecoration.OBFUSCATED ? "" : "<" + format.decoration() + ">";
        });
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
