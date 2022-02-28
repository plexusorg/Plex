package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.Plex;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.CommandFailException;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.rank.enums.Rank;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandPermissions(level = Rank.OP, permission = "plex.plex", source = RequiredCommandSource.ANY)
@CommandParameters(name = "plex", usage = "/<command> [reload | redis]", aliases = "plexhelp", description = "Show information about Plex or reload it")
public class PlexCMD extends PlexCommand
{
    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length == 0)
        {
            send(sender, ChatColor.LIGHT_PURPLE + "Plex - A new freedom plugin.");
            return componentFromString(ChatColor.LIGHT_PURPLE + "Plugin version: " + ChatColor.GOLD + plugin.getDescription().getVersion());
        }
        if (args[0].equalsIgnoreCase("reload"))
        {
            checkRank(sender, Rank.SENIOR_ADMIN, "plex.reload");

            Plex.get().config.load();
            send(sender, "Reloaded config file");

            Plex.get().messages.load();
            send(sender, "Reloaded messages file");

            Plex.get().indefBans.load();
            Plex.get().getPunishmentManager().mergeIndefiniteBans();
            send(sender, "Reloaded indefinite bans");

            Plex.get().getRankManager().importDefaultRanks();
            send(sender, "Imported ranks");

            send(sender, "Plex successfully reloaded.");
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
        return ImmutableList.of("reload", "redis");
    }
}