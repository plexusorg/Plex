package dev.plex.util;

import dev.plex.Plex;
import dev.plex.PlexBase;
import dev.plex.cache.DataUtils;
import dev.plex.cache.PlayerCache;
import dev.plex.permission.Permission;
import dev.plex.player.PlexPlayer;
import dev.plex.storage.StorageType;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.Month;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommandYamlParser;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class PlexUtils implements PlexBase
{
    private static final Random RANDOM;
    public static Map<String, ChatColor> CHAT_COLOR_NAMES;
    public static List<ChatColor> CHAT_COLOR_POOL;
    public static List<String> DEVELOPERS =
            Arrays.asList("78408086-1991-4c33-a571-d8fa325465b2", // Telesphoreo
                    "f5cd54c4-3a24-4213-9a56-c06c49594dff", // Taahh
                    "ca83b658-c03b-4106-9edc-72f70a80656d", // ayunami2000
                    "2e06e049-24c8-42e4-8bcf-d35372af31e6" //Fleek
            );
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd/yyyy 'at' hh:mm:ss a z");
    private static final Set<String> TIMEZONES = Set.of(TimeZone.getAvailableIDs());
    private static String TIMEZONE = Plex.get().config.getString("server.timezone");

    static
    {
        RANDOM = new Random();
        CHAT_COLOR_NAMES = new HashMap<>();
        CHAT_COLOR_POOL = Arrays.asList(ChatColor.DARK_RED, ChatColor.RED, ChatColor.GOLD, ChatColor.YELLOW, ChatColor.GREEN, ChatColor.DARK_GREEN, ChatColor.AQUA, ChatColor.DARK_AQUA, ChatColor.BLUE, ChatColor.DARK_BLUE, ChatColor.DARK_PURPLE, ChatColor.LIGHT_PURPLE);
        for (final ChatColor chatColor : CHAT_COLOR_POOL)
        {
            CHAT_COLOR_NAMES.put(chatColor.name().toLowerCase().replace("_", ""), chatColor);
        }
    }

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
        Particle.CLOUD.builder().location(location).receivers(players).extra(0).offset(0.5, 0.5, 0.5).count(5).spawn();
        Particle.FLAME.builder().location(location).receivers(players).extra(0).offset(0.5, 0.5, 0.5).count(3).spawn();
        Particle.SOUL_FIRE_FLAME.builder().location(location).receivers(players).offset(0.5, 0.5, 0.5).extra(0).count(2).spawn();
        // note that the sound is played to everyone who is close enough to hear it
        players[0].getWorld().playSound(location, org.bukkit.Sound.BLOCK_FIRE_EXTINGUISH, 0.5f, 0.5f);
    }

    public static ChatColor randomChatColor()
    {
        return CHAT_COLOR_POOL.get(RANDOM.nextInt(CHAT_COLOR_POOL.size()));
    }

    public static void testConnections()
    {
        if (Plex.get().getSqlConnection().getDataSource() != null)
        {
            try (Connection con = Plex.get().getSqlConnection().getCon())
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
                if (Plex.get().getMongoConnection().getDatastore() != null)
                {
                    PlexLog.log("Successfully enabled MongoDB!");
                }
            }
        }
        else
        {
            if (Plex.get().getMongoConnection().getDatastore() != null)
            {
                PlexLog.log("Successfully enabled MongoDB!");
            }
        }
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
            List<String> cmdAliases = pluginCmd.getAliases().size() > 0 ? pluginCmd.getAliases().stream().map(String::toLowerCase).collect(Collectors.toList()) : null;
            if (pluginCmd.getName().equalsIgnoreCase(cmd) || (cmdAliases != null && cmdAliases.contains(cmd.toLowerCase())))
            {
                return true;
            }
        }
        return false;
    }

    public static String colorize(final String string)
    {
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    private static final MiniMessage safeMessage = MiniMessage.builder().tags(TagResolver.builder().resolvers(
            StandardTags.color(),
            StandardTags.decorations(),
            StandardTags.gradient(),
            StandardTags.rainbow(),
            StandardTags.reset()
    ).build()).build();

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
        LocalDateTime date = LocalDateTime.now();
        if (aprilFools && date.getMonth() == Month.APRIL && date.getDayOfMonth() == 1)
        {
            Component component = MiniMessage.miniMessage().deserialize(input); // removes existing tags
            return MiniMessage.miniMessage().deserialize("<rainbow>" + PlainTextComponentSerializer.plainText().serialize(component));
        }
        return MiniMessage.miniMessage().deserialize(input);
    }

    public static Component mmCustomDeserialize(String input, TagResolver... resolvers)
    {
        return MiniMessage.builder().tags(TagResolver.builder().resolvers(resolvers).build()).build().deserialize(input);
    }

    public static Component messageComponent(String entry, Object... objects)
    {
        return MiniMessage.miniMessage().deserialize(messageString(entry, objects));
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

    public static String useTimezone(LocalDateTime date)
    {
        // Use UTC if the timezone is null or not set correctly
        if (TIMEZONE == null || !TIMEZONES.contains(TIMEZONE))
        {
            TIMEZONE = "Etc/UTC";
        }
        return DATE_FORMAT.withZone(ZoneId.of(TIMEZONE)).format(date);
    }


    public static List<String> getPlayerNameList()
    {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
    }

    public static void broadcast(String s)
    {
        Bukkit.broadcast(MiniMessage.miniMessage().deserialize(s));
    }

    public static void broadcast(Component component)
    {
        Bukkit.broadcast(component);
    }

    public static void broadcastToAdmins(Component component)
    {
        Bukkit.getOnlinePlayers().stream().filter(pl -> PlayerCache.getPlexPlayer(pl.getUniqueId()).isAdminActive()).forEach(pl ->
        {
            pl.sendMessage(component);
        });
    }

    public static boolean randomBoolean()
    {
        return ThreadLocalRandom.current().nextBoolean();
    }

    public static int randomNum()
    {
        return ThreadLocalRandom.current().nextInt();
    }

    public static int randomNum(int limit)
    {
        return ThreadLocalRandom.current().nextInt(limit);
    }

    public static int randomNum(int start, int limit)
    {
        return ThreadLocalRandom.current().nextInt(start, limit);
    }
}
