package dev.plex.util;

import com.google.common.base.CharMatcher;
import com.google.common.collect.Lists;
import dev.plex.Plex;
import dev.plex.PlexBase;
import dev.plex.listener.impl.ChatListener;
import dev.plex.storage.StorageType;
import dev.plex.util.minimessage.SafeMiniMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommandYamlParser;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class PlexUtils implements PlexBase
{
    private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();

    public static List<String> DEVELOPERS =
            Arrays.asList("78408086-1991-4c33-a571-d8fa325465b2", // Telesphoreo
                    "f5cd54c4-3a24-4213-9a56-c06c49594dff", // Taahh
                    "53b1512e-3481-4702-9f4f-63cb9c8be6a1", // supernt
                    "ca83b658-c03b-4106-9edc-72f70a80656d", // ayunami2000
                    "2e06e049-24c8-42e4-8bcf-d35372af31e6", // Fleek
                    "a52f1f08-a398-400a-bca4-2b74b81feae6" // Allink
            );

    private static final Pattern LEGACY_FORMATTING_PATTERN = Pattern.compile(".*(?i)(([§&])((#[a-f0-9]{3,6})|([0-9a-fklmnor]))).*");

    public static <T> T addToArrayList(List<T> list, T object)
    {
        list.add(object);
        return object;
    }

    public static void disabledEffect(Player player, Location location)
    {
        Particle.CLOUD.builder().location(location).receivers(player).extra(0).offset(0.5, 0.5, 0.5).count(5).spawn();
        Particle.FLAME.builder().location(location).receivers(player).extra(0).offset(0.5, 0.5, 0.5).count(3).spawn();
        Particle.SOUL_FIRE_FLAME.builder().location(location).receivers(player).offset(0.5, 0.5, 0.5).extra(0).count(2).spawn();
        player.playSound(location, org.bukkit.Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 0.5f);
    }

    public static void disabledEffectMultiple(Player[] players, Location location)
    {
        if (players.length < 1)
        {
            return;
        }

        Particle.CLOUD.builder().location(location).receivers(players).extra(0).offset(0.5, 0.5, 0.5).count(5).spawn();
        Particle.FLAME.builder().location(location).receivers(players).extra(0).offset(0.5, 0.5, 0.5).count(3).spawn();
        Particle.SOUL_FIRE_FLAME.builder().location(location).receivers(players).offset(0.5, 0.5, 0.5).extra(0).count(2)
                .spawn();
        // note that the sound is played to everyone who is close enough to hear it
        players[0].getWorld().playSound(location, org.bukkit.Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 0.5f);
    }

    public static void testConnections()
    {
        if (Plex.get().getSqlConnection().getDataSource() != null)
        {
            try (Connection ignored = Plex.get().getSqlConnection().getCon())
            {
                if (Plex.get().getStorageType() == StorageType.MARIADB)
                {
                    PlexLog.log("Successfully enabled MySQL!");
                }
                else if (Plex.get().getStorageType() == StorageType.SQLITE)
                {
                    PlexLog.log("Successfully enabled SQLite!");
                }
            }
            catch (SQLException e)
            {
                PlexLog.error("Unable to connect to the SQL Server");
            }
        }
        else
        {
            PlexLog.error("Unable to initialize hikari data source!");
        }
    }

    public static boolean isFolia()
    {
        try
        {
            Class.forName("io.papermc.paper.threadedregions.ThreadedRegionizer");
        }
        catch (Exception e)
        {
            return false;
        }

        return true;
    }

    public static boolean isPluginCMD(String cmd, String pluginName)
    {
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin(pluginName);
        if (plugin == null)
        {
            PlexLog.error(pluginName + " can not be found on the server! Make sure it is spelt correctly!");
            return false;
        }
        List<Command> cmds = PluginCommandYamlParser.parse(plugin);
        for (Command pluginCmd : cmds)
        {
            List<String> cmdAliases = pluginCmd.getAliases().size() > 0 ? pluginCmd.getAliases().stream().map(String::toLowerCase).toList() : null;
            if (pluginCmd.getName().equalsIgnoreCase(cmd) || (cmdAliases != null && cmdAliases.contains(cmd.toLowerCase())))
            {
                return true;
            }
        }
        return false;
    }

    public static Component removeHoverAndClick(Component component)
    {
        Stack<Component> components = new Stack<>();
        components.push(component);
        while (!components.isEmpty())
        {
            Component curr = components.pop();
            curr.clickEvent(null).hoverEvent(null);
            curr.children().forEach(components::push);
        }
        return component;
    }

    public static Component stringToComponent(String input)
    {
        input = cleanString(input);

        return LEGACY_FORMATTING_PATTERN.matcher(input).find() ?
                LegacyComponentSerializer.legacyAmpersand().deserialize(input.replaceAll("([§&]+)(k+)", "") // Ugly hack, but it tries to prevent &k and any attempts to bypass it.
                        ).decoration(TextDecoration.OBFUSCATED, TextDecoration.State.FALSE) :
                SafeMiniMessage.mmDeserializeWithoutEvents(input);
    }

    @Deprecated
    public static String legacyToMiniString(String input)
    {
        return cleanString(input.replace("&a", "<green>")
                .replace("&b", "<aqua>")
                .replace("&c", "<red>")
                .replace("&d", "<light_purple>")
                .replace("&e", "<yellow>")
                .replace("&f", "<white>")
                .replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>")
                .replace("&5", "<dark_purple>")
                .replace("&6", "<gold>")
                .replace("&7", "<gray>")
                .replace("&8", "<dark_gray>")
                .replace("&9", "<blue>")
                .replace("&0", "<black>")
                .replace("&r", "<reset>")
                .replace("&l", "<bold>")
                .replace("&o", "<italic>")
                .replace("&n", "<underlined>")
                .replace("&m", "<strikethrough>")
                .replace("&k", "<obf>"));
    }

    public static String mmStripColor(String input)
    {
        return PlainTextComponentSerializer.plainText().serialize(mmDeserialize(input));
    }

    public static Component mmDeserialize(String input)
    {
        boolean aprilFools = true; // true by default
        if (plugin.config.contains("april_fools"))
        {
            aprilFools = plugin.config.getBoolean("april_fools");
        }
        ZonedDateTime date = ZonedDateTime.now(ZoneId.systemDefault());
        if (aprilFools && date.getMonth() == Month.APRIL && date.getDayOfMonth() == 1)
        {
            Component component = MINI_MESSAGE.deserialize(input); // removes existing tags
            return MINI_MESSAGE.deserialize("<rainbow>" + PlainTextComponentSerializer.plainText().serialize(component));
        }
        return MINI_MESSAGE.deserialize(input);
    }

    public static String mmSerialize(Component input)
    {
        return MINI_MESSAGE.serialize(input);
    }

    public static Component mmCustomDeserialize(String input, TagResolver... resolvers)
    {
        return MiniMessage.builder().tags(TagResolver.builder().resolvers(resolvers).build()).build().deserialize(input);
    }

    public static Component messageComponent(String entry, Object... objects)
    {
        return MINI_MESSAGE.deserialize(messageString(entry, objects));
    }

    public static Component messageComponent(String entry, Component... objects)
    {
        Component component = MINI_MESSAGE.deserialize(messageString(entry));
        for (int i = 0; i < objects.length; i++)
        {
            int finalI = i;
            component = component.replaceText(builder -> builder.matchLiteral("{" + finalI + "}").replacement(objects[finalI]).build());
        }
        return component;
    }

    public static String messageString(String entry, Object... objects)
    {
        String f = plugin.messages.getString(entry);
        if (f == null)
        {
            throw new NullPointerException();
        }
        for (int i = 0; i < objects.length; i++)
        {
            f = f.replace("{" + i + "}", String.valueOf(objects[i]));
        }
        return f;
    }


    public static String getTextFromComponent(Component component)
    {
        try
        {
            return ((TextComponent) component).content();
        }
        catch (Exception e)
        {
            PlexLog.warn("Unable to get text of component", e.getLocalizedMessage());
            return "";
        }
    }

    public static String getTextFromComponents(Component... components)
    {
        try
        {
            StringBuilder builder = new StringBuilder();

            for (Component component : components)
            {
                builder.append(getTextFromComponent(component));
            }

            return builder.toString();
        }
        catch (Exception e)
        {
            PlexLog.warn("Unable to get text of components", e.getLocalizedMessage());
            return "";
        }
    }

    public static List<String> getPlayerNameList()
    {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
    }

    public static void broadcast(String s)
    {
        Bukkit.broadcast(MINI_MESSAGE.deserialize(s));
    }

    public static void broadcast(Component component)
    {
        Bukkit.broadcast(component);
    }

    public static void broadcastToAdmins(Component component, String permission)
    {
        Bukkit.getOnlinePlayers().stream().filter(pl -> pl.hasPermission(permission)).forEach(pl ->
        {
            pl.sendMessage(component);
        });
    }

    public static List<UUID> adminChat(String senderName, String prefix, String message, UUID... ignore)
    {
        List<UUID> sent = Lists.newArrayList();
        for (Player player : Bukkit.getOnlinePlayers())
        {
            if (Arrays.stream(ignore).anyMatch(uuid -> player.getUniqueId().equals(uuid)))
            {
                continue;
            }
            if (player.hasPermission("plex.adminchat"))
            {
                player.sendMessage(PlexUtils.messageComponent("adminChatFormat", senderName, prefix, message).replaceText(ChatListener.URL_REPLACEMENT_CONFIG));
                sent.add(player.getUniqueId());
            }
        }
        return sent;
    }

    public static String cleanString(String input)
    {
        return CharMatcher.forPredicate(c -> Character.getDirectionality(c) != Character.DIRECTIONALITY_RIGHT_TO_LEFT_OVERRIDE && Character.getDirectionality(c) != Character.DIRECTIONALITY_RIGHT_TO_LEFT).retainFrom(input);
    }
}
