package me.totalfreedom.plex.util;

import java.sql.SQLException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import me.totalfreedom.plex.Plex;
import me.totalfreedom.plex.config.Config;
import me.totalfreedom.plex.storage.StorageType;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommandYamlParser;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class PlexUtils
{
    public static void testConnections()
    {
        if (Plex.get().getSqlConnection().getCon() != null)
        {
            if (Plex.get().getStorageType() == StorageType.SQL)
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

    // if you think the name of this method is dumb feel free to change it i just thought it'd be cool
    public static String tl(String s, Object... objects)
    {
        Plex plugin = Plex.get();
        if (s.equals("baseColor") || s.equals("errorColor") || s.equals("broadcastColor"))
            return getChatColorFromConfig(plugin.messages, ChatColor.WHITE, s).toString();
        String f = plugin.messages.getString(s);
        if (f == null)
            return ChatColor.RED + "No message";
        for (Object object : objects)
            f = f.replace("<v>", String.valueOf(object));
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
            color = def;
        else if (ChatColor.getByChar(config.getString(path)) == null)
            color = def;
        else
            color = ChatColor.getByChar(config.getString(path));
        return color;
    }

    public static void warpToWorld(Player player, World world)
    {
        player.teleport(new Location(world, 0, world.getHighestBlockYAt(0, 0), 0));
    }

    public static void setBlocks(Location c1, Location c2, Material material)
    {
        if (!c1.getWorld().getName().equals(c1.getWorld().getName()))
            return;
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

    public static void broadcast(String s)
    {
        Bukkit.broadcastMessage(s);
    }
}
