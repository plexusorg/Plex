package dev.plex.util;

import dev.plex.Plex;
import dev.plex.config.Config;
import dev.plex.storage.StorageType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommandYamlParser;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class PlexUtils
{
    public static Map<String, ChatColor> CHAT_COLOR_NAMES;
    public static List<ChatColor> CHAT_COLOR_POOL;
    private static Random RANDOM;

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

    public static ChatColor randomChatColor()
    {
        return CHAT_COLOR_POOL.get(RANDOM.nextInt(CHAT_COLOR_POOL.size()));
    }

    public static void testConnections()
    {
        if (Plex.get().getSqlConnection().getCon() != null)
        {
            if (Plex.get().getStorageType() == StorageType.MARIADB)
            {
                PlexLog.log("Successfully enabled MySQL!");
            }
            else if (Plex.get().getStorageType() == StorageType.SQLITE)
            {
                PlexLog.log("Successfully enabled SQLite!");
            }
            try
            {
                Plex.get().getSqlConnection().getCon().close();
            }
            catch (SQLException ignored)
            {
            }
        }
        else if (Plex.get().getMongoConnection().getDatastore() != null)
        {
            PlexLog.log("Successfully enabled MongoDB!");
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

    public static String color(String s)
    {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static String colorize(final String string)
    {
        return ChatColor.translateAlternateColorCodes('&', string);
    }

    // if you think the name of this method is dumb feel free to change it i just thought it'd be cool
    public static String tl(String s, Object... objects)
    {
        Plex plugin = Plex.get();
        if (s.equals("baseColor") || s.equals("errorColor") || s.equals("broadcastColor"))
        {
            return getChatColorFromConfig(plugin.messages, ChatColor.WHITE, s).toString();
        }
        String f = plugin.messages.getString(s);
        if (f == null)
        {
            return ChatColor.RED + "No message";
        }
        for (Object object : objects)
        {
            f = f.replaceFirst("<v>", String.valueOf(object));
        }
        ChatColor base = getChatColorFromConfig(plugin.messages, ChatColor.GRAY, "baseColor");
        ChatColor broadcast = getChatColorFromConfig(plugin.messages, ChatColor.AQUA, "broadcastColor");
        ChatColor error = getChatColorFromConfig(plugin.messages, ChatColor.RED, "errorColor");
        f = f.replaceAll("<r>", base.toString());
        f = f.replaceAll("<b>", broadcast.toString());
        f = f.replaceAll("<e>", error.toString());
        f = color(f);
        return base + f;
    }

    public static ChatColor getChatColorFromConfig(Config config, ChatColor def, String path)
    {
        ChatColor color;
        if (config.getString(path) == null)
        {
            color = def;
        }
        else if (ChatColor.getByChar(config.getString(path)) == null)
        {
            color = def;
        }
        else
        {
            color = ChatColor.getByChar(config.getString(path));
        }
        return color;
    }

    public static void setBlocks(Location c1, Location c2, Material material)
    {
        if (!c1.getWorld().getName().equals(c1.getWorld().getName()))
        {
            return;
        }
        int sy = Math.min(c1.getBlockY(), c2.getBlockY()),
                ey = Math.max(c1.getBlockY(), c2.getBlockY()),
                sx = Math.min(c1.getBlockX(), c2.getBlockX()),
                ex = Math.max(c1.getBlockX(), c2.getBlockX()),
                sz = Math.min(c1.getBlockZ(), c2.getBlockZ()),
                ez = Math.max(c1.getBlockZ(), c2.getBlockZ());
        World world = c1.getWorld();
        for (int y = sy; y <= ey; y++)
        {
            for (int x = sx; x <= ex; x++)
            {
                for (int z = sz; z <= ez; z++)
                {
                    world.getBlockAt(x, y, z).setType(material);
                }
            }
        }
    }

    public static List<String> getPlayerNameList()
    {
        List<String> names = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers())
        {
            names.add(player.getName());
        }
        return names;
    }

    public static void broadcast(String s)
    {
        Bukkit.broadcastMessage(s);
    }

    public static Object simpleGET(String url) throws IOException, ParseException
    {
        URL u = new URL(url);
        HttpURLConnection connection = (HttpURLConnection)u.openConnection();
        connection.setRequestMethod("GET");
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String line;
        StringBuilder content = new StringBuilder();
        while ((line = in.readLine()) != null)
        {
            content.append(line);
        }
        in.close();
        connection.disconnect();
        return new JSONParser().parse(content.toString());
    }

    public static UUID getFromName(String name)
    {
        JSONObject profile;
        try
        {
            profile = (JSONObject)PlexUtils.simpleGET("https://api.ashcon.app/mojang/v2/user/" + name);
        }
        catch (IOException | ParseException e)
        {
            e.printStackTrace();
            return null;
        }
        String uuidString = (String)profile.get("uuid");
        return UUID.fromString(uuidString);
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

    public static long getDateNow()
    {
        return new Date().getTime();
    }

    public static Date getDateFromLong(long epoch)
    {
        return new Date(epoch);
    }

    public static long hoursToSeconds(long hours)
    {
        return hours * 60 * 60;
    }

    public static long minutesToSeconds(long minutes)
    {
        return minutes * 60;
    }
}
