package dev.plex.command.impl.brigadier;

import com.mojang.brigadier.arguments.StringArgumentType;
import dev.plex.command.PlexBrigadierCommand;
import dev.plex.command.annotation.*;
import dev.plex.command.exception.CommandFailException;
import dev.plex.module.PlexModule;
import dev.plex.module.PlexModuleFile;
import dev.plex.util.BuildInfo;
import dev.plex.util.TimeUtils;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.command.CommandSender;

import java.util.stream.Collectors;

/**
 * @author Taah
 * @since 3:46 PM [07-07-2023]
 */
@CommandName({"plex", "pplexx"})
public class PlexBrigadierCMD extends PlexBrigadierCommand
{
    @SubCommand("reload")
    @CommandPermission("plex.reload")
    public void reloadPlex(CommandSender sender)
    {
        plugin.config.load();
        send(sender, "Reloaded config file");
        plugin.messages.load();
        send(sender, "Reloaded messages file");
        plugin.indefBans.load(false);
        plugin.getPunishmentManager().mergeIndefiniteBans();
        send(sender, "Reloaded indefinite bans");
        plugin.commands.load();
        send(sender, "Reloaded blocked commands file");
        plugin.getRankManager().importDefaultRanks();
        send(sender, "Imported ranks");
        plugin.setSystem(plugin.config.getString("system"));
        if (plugin.getSystem().equalsIgnoreCase("permissions") && !plugin.getServer().getPluginManager().isPluginEnabled("Vault"))
        {
            throw new RuntimeException("Vault is required to run on the server if you use permissions!");
        }
        plugin.getServiceManager().endServices();
        plugin.getServiceManager().startServices();
        send(sender, "Restarted services.");
        TimeUtils.TIMEZONE = plugin.config.getString("server.timezone");
        send(sender, "Set timezone to: " + TimeUtils.TIMEZONE);
        send(sender, "Plex successfully reloaded.");
    }

    @SubCommand("test yourmom")
    @CommandPermission("plex.test")
    public void testPlex(CommandSender sender, @Argument(value = "test", min =  0, max = 100) int i, @Argument(value = "test2", min =  0, max = 100) int j, @Argument(value = "param", argumentType = StringArgumentType.StringType.GREEDY_PHRASE) String param) {
        send(sender, String.valueOf(i));
        send(sender, String.valueOf(j));
//        send(sender, param);
    }

    @SubCommand("redis")
    @CommandPermission("plex.redis")
    public void testRedis(CommandSender sender)
    {
        if (!plugin.getRedisConnection().isEnabled())
        {
            throw new CommandFailException("&cRedis is not enabled.");
        }
        plugin.getRedisConnection().getJedis().set("test", "123");
        send(sender, "Set test to 123. Now outputting key test...");
        send(sender, plugin.getRedisConnection().getJedis().get("test"));
        plugin.getRedisConnection().getJedis().close();
    }

    @SubCommand("modules")
    @CommandPermission("plex.modules")
    public void viewModules(CommandSender sender)
    {
        send(sender, mmString("<gold>Modules (" + plugin.getModuleManager().getModules().size() + "): <yellow>" + StringUtils.join(plugin.getModuleManager().getModules().stream().map(PlexModule::getPlexModuleFile).map(PlexModuleFile::getName).collect(Collectors.toList()), ", ")));
    }

    @SubCommand("modules reload")
    @CommandPermission("plex.modules.reload")
    public void reloadModules(CommandSender sender)
    {
        plugin.getModuleManager().reloadModules();
        send(sender, mmString("<green>All modules reloaded!"));
    }

    @SubCommand("modules testing more args")
    @CommandPermission("plex.modules.reload")
    public void stupidTesting(CommandSender sender)
    {
        send(sender, "leave me alone");
//        send(sender, mmString("<gold>Modules (" + plugin.getModuleManager().getModules().size() + "): <yellow>" + StringUtils.join(plugin.getModuleManager().getModules().stream().map(PlexModule::getPlexModuleFile).map(PlexModuleFile::getName).collect(Collectors.toList()), ", ")));

    }

    @Default
    public void defaultCommand(CommandSender sender)
    {
        send(sender, mmString("<light_purple>Plex - A new freedom plugin."));
        send(sender, mmString("<light_purple>Plugin version: <gold>" + plugin.getPluginMeta().getVersion() + " #" + BuildInfo.getNumber() + " <light_purple>Git: <gold>" + BuildInfo.getHead()));
        send(sender, mmString("<light_purple>Authors: <gold>Telesphoreo, Taahh"));
        send(sender, mmString("<light_purple>Built by: <gold>" + BuildInfo.getAuthor() + " <light_purple>on <gold>" + BuildInfo.getDate()));
        send(sender, mmString("<light_purple>Run <gold>/plex modules <light_purple>to see a list of modules."));
        plugin.getUpdateChecker().getUpdateStatusMessage(sender, true, 2);
    }
}
