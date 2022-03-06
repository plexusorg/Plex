package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.CommandFailException;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.module.PlexModule;
import dev.plex.module.PlexModuleFile;
import dev.plex.rank.enums.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.stream.Collectors;

@CommandPermissions(level = Rank.OP, permission = "plex.plex", source = RequiredCommandSource.ANY)
@CommandParameters(name = "plex", usage = "/<command> [reload | redis | modules] [reload]", aliases = "plexhelp", description = "Show information about Plex or reload it")
public class PlexCMD extends PlexCommand {
    // Don't modify this command
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args) {
        if (args.length == 0) {
            send(sender, ChatColor.LIGHT_PURPLE + "Plex - A new freedom plugin.");
            send(sender, ChatColor.LIGHT_PURPLE + "Plugin version: " + plugin.getDescription().getVersion());
            return componentFromString(ChatColor.LIGHT_PURPLE + "Authors: " + ChatColor.GOLD + "Telesphoreo, Taahh");
        }
        if (args[0].equalsIgnoreCase("reload")) {
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
        } else if (args[0].equalsIgnoreCase("redis")) {
            checkRank(sender, Rank.SENIOR_ADMIN, "plex.redis");
            if (!plugin.getRedisConnection().isEnabled()) {
                throw new CommandFailException("&cRedis is not enabled.");
            }
            plugin.getRedisConnection().getJedis().set("test", "123");
            send(sender, "Set test to 123. Now outputting key test...");
            send(sender, plugin.getRedisConnection().getJedis().get("test"));
            plugin.getRedisConnection().getJedis().close();
        }
        if (args[0].equalsIgnoreCase("modules")) {
            if (args.length == 1) {
                return MiniMessage.miniMessage().deserialize("<gold>Modules (" + plugin.getModuleManager().getModules().size() + "): <yellow>" + StringUtils.join(plugin.getModuleManager().getModules().stream().map(PlexModule::getPlexModuleFile).map(PlexModuleFile::getName).collect(Collectors.toList()), ", "));
            }
            if (args[1].equalsIgnoreCase("reload")) {
                plugin.getModuleManager().unloadModules();
                plugin.getModuleManager().loadAllModules();
                plugin.getModuleManager().loadModules();
                plugin.getModuleManager().enableModules();
            }
        } else {
            return usage();
        }
        return null;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        return ImmutableList.of("reload", "redis");
    }
}