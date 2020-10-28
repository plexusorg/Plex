package me.totalfreedom.plex.util;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import me.totalfreedom.plex.Plex;
import me.totalfreedom.plex.storage.StorageType;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommandYamlParser;
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

}
