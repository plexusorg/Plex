package dev.plex.command.impl;

import dev.plex.Plex;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.CommandFailException;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.module.PlexModule;
import dev.plex.module.PlexModuleFile;
import dev.plex.rank.enums.Rank;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import dev.plex.util.PlexLog;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang.StringUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandPermissions(level = Rank.OP, permission = "plex.plex", source = RequiredCommandSource.ANY)
@CommandParameters(name = "plex", usage = "/<command> [reload | redis | modules [reload]]", description = "Show information about Plex or reload it")
public class PlexCMD extends PlexCommand
{
    // Don't modify this command
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length == 0)
        {
            send(sender, mmString("<light_purple>Plex - A new freedom plugin."));
            send(sender, mmString("<light_purple>Plugin version: <gold>" + plugin.getDescription().getVersion() + " #" + Plex.build.number + " <light_purple>Git: <gold>" + Plex.build.head));
            send(sender, mmString("<light_purple>Authors: <gold>Telesphoreo, Taahh"));
            send(sender, mmString("<light_purple>Built by: <gold>" + Plex.build.author + " <light_purple>on <gold>" + Plex.build.date));
            send(sender, mmString("<light_purple>Run <gold>/plex modules <light_purple>to see a list of modules."));
            plugin.getUpdateChecker().getUpdateStatusMessage(sender, true);
            return null;
        }
        if (args[0].equalsIgnoreCase("reload"))
        {
            checkRank(sender, Rank.SENIOR_ADMIN, "plex.reload");
            plugin.config.load();
            send(sender, "Reloaded config file");
            plugin.messages.load();
            send(sender, "Reloaded messages file");
            plugin.indefBans.load(false);
            plugin.getPunishmentManager().mergeIndefiniteBans();
            send(sender, "Reloaded indefinite bans");
            plugin.getRankManager().importDefaultRanks();
            send(sender, "Imported ranks");
            send(sender, "Plex successfully reloaded.");
            plugin.setSystem(plugin.config.getString("system"));
            plugin.getServiceManager().endServices();
            plugin.getServiceManager().startServices();
            PlexLog.debug("Restarted services");
            return null;
        }
        else if (args[0].equalsIgnoreCase("redis"))
        {
            checkRank(sender, Rank.SENIOR_ADMIN, "plex.redis");
            if (!plugin.getRedisConnection().isEnabled())
            {
                throw new CommandFailException("&cRedis is not enabled.");
            }
            plugin.getRedisConnection().getJedis().set("test", "123");
            send(sender, "Set test to 123. Now outputting key test...");
            send(sender, plugin.getRedisConnection().getJedis().get("test"));
            plugin.getRedisConnection().getJedis().close();
            return null;
        }
        else if (args[0].equalsIgnoreCase("modules"))
        {
            if (args.length == 1)
            {
                return mmString("<gold>Modules (" + plugin.getModuleManager().getModules().size() + "): <yellow>" + StringUtils.join(plugin.getModuleManager().getModules().stream().map(PlexModule::getPlexModuleFile).map(PlexModuleFile::getName).collect(Collectors.toList()), ", "));
            }
            if (args[1].equalsIgnoreCase("reload"))
            {
                checkRank(sender, Rank.SENIOR_ADMIN, "plex.modules.reload");
                plugin.getModuleManager().unloadModules();
                plugin.getModuleManager().loadAllModules();
                plugin.getModuleManager().loadModules();
                plugin.getModuleManager().enableModules();
                return componentFromString("All modules reloaded!");
            }
        }
        else
        {
            return usage();
        }
        return null;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        if (args.length == 1)
        {
            return Arrays.asList("reload", "redis", "modules");
        }
        else if (args[0].equalsIgnoreCase("modules"))
        {
            return List.of("reload");
        }
        return Collections.emptyList();
    }
}