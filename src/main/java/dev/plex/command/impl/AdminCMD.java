package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.cache.DataUtils;
import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.ConsoleOnlyException;
import dev.plex.command.exception.PlayerNotFoundException;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.event.AdminAddEvent;
import dev.plex.event.AdminRemoveEvent;
import dev.plex.event.AdminSetRankEvent;
import dev.plex.player.PlexPlayer;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandPermissions(level = Rank.OP, source = RequiredCommandSource.ANY)
@CommandParameters(name = "admin", usage = "/<command> <add | remove | setrank | list> [player] [rank]", aliases = "saconfig,slconfig,adminconfig,adminmanage", description = "Manage all admins")
public class AdminCMD extends PlexCommand
{
    //TODO: Better return messages

    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length == 0)
        {
            return usage();
        }

        if (args[0].equalsIgnoreCase("add"))
        {
            if (args.length != 2)
            {
                return usage("/admin add <player>");
            }

            if (!isConsole(sender))
            {
                throw new ConsoleOnlyException();
            }

            UUID targetUUID = PlexUtils.getFromName(args[1]);

            if (targetUUID == null || !DataUtils.hasPlayedBefore(targetUUID))
            {
                throw new PlayerNotFoundException();
            }
            PlexPlayer plexPlayer = DataUtils.getPlayer(targetUUID);

            if (isAdmin(plexPlayer))
            {
                return tl("playerIsAdmin");
            }

            Bukkit.getServer().getPluginManager().callEvent(new AdminAddEvent(sender, plexPlayer));
            return null;
        }
        if (args[0].equalsIgnoreCase("remove"))
        {
            if (args.length != 2)
            {
                return usage("/admin remove <player>");
            }

            if (!isConsole(sender))
            {
                throw new ConsoleOnlyException();
            }

            UUID targetUUID = PlexUtils.getFromName(args[1]);

            if (targetUUID == null || !DataUtils.hasPlayedBefore(targetUUID))
            {
                throw new PlayerNotFoundException();
            }
            PlexPlayer plexPlayer = DataUtils.getPlayer(targetUUID);

            if (!isAdmin(plexPlayer))
            {
                return tl("playerNotAdmin");
            }

            Bukkit.getServer().getPluginManager().callEvent(new AdminRemoveEvent(sender, plexPlayer));
            return null;
        }

        if (args[0].equalsIgnoreCase("setrank"))
        {
            if (args.length != 3)
            {
                return usage("/admin setrank <player> <rank>");
            }

            if (!isConsole(sender))
            {
                throw new ConsoleOnlyException();
            }

            UUID targetUUID = PlexUtils.getFromName(args[1]);

            if (targetUUID == null || !DataUtils.hasPlayedBefore(targetUUID))
            {
                throw new PlayerNotFoundException();
            }

            if (!rankExists(args[2]))
            {
                return tl("rankNotFound");
            }

            Rank rank = Rank.valueOf(args[2].toUpperCase());

            if (!rank.isAtLeast(Rank.ADMIN))
            {
                return tl("rankMustBeHigherThanAdmin");
            }

            PlexPlayer plexPlayer = DataUtils.getPlayer(targetUUID);

            if (!isAdmin(plexPlayer))
            {
                return tl("playerNotAdmin");
            }

            Bukkit.getServer().getPluginManager().callEvent(new AdminSetRankEvent(sender, plexPlayer, rank));

            return null;
        }

        if (args[0].equalsIgnoreCase("list"))
        {
            if (args.length != 1)
            {
                return usage("/admin list");
            }

            return componentFromString("Admins: " + StringUtils.join(plugin.getAdminList().getAllAdmins(), ", "));
        }
        return null;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        if (args.length == 1)
        {
            return Arrays.asList("add", "remove", "setrank", "list");
        }
        else if (args.length == 2 && !args[0].equalsIgnoreCase("list"))
        {
            return PlexUtils.getPlayerNameList();
        }
        return ImmutableList.of();
    }


    private boolean rankExists(String rank)
    {
        for (Rank ranks : Rank.values())
        {
            if (ranks.name().equalsIgnoreCase(rank))
            {
                return true;
            }
        }
        return false;
    }
}
