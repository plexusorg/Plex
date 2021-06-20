package dev.plex.command.impl;

import com.google.common.collect.ImmutableList;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.ConsoleOnlyException;
import dev.plex.command.exception.PlayerNotFoundException;
import dev.plex.command.source.CommandSource;
import dev.plex.command.source.RequiredCommandSource;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import dev.plex.cache.DataUtils;
import dev.plex.command.PlexCommand;
import dev.plex.event.AdminAddEvent;
import dev.plex.event.AdminRemoveEvent;
import dev.plex.event.AdminSetRankEvent;
import dev.plex.player.PlexPlayer;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;

@CommandPermissions(level = Rank.SENIOR_ADMIN, source = RequiredCommandSource.ANY)
@CommandParameters(usage = "/<command> <add | remove | setrank | list> [player] [rank]", aliases = "saconfig,slconfig,adminconfig,adminmanage", description = "Manage all admins")
public class AdminCMD extends PlexCommand
{
    //TODO: Better return messages

    public AdminCMD()
    {
        super("admin");
    }

    @Override
    public void execute(CommandSource sender, String[] args)
    {
        if (args.length == 0)
        {
            sender.send(usage(getUsage()));
            return;
        }

        if (args[0].equalsIgnoreCase("add"))
        {
            if (args.length != 2)
            {
                sender.send(usage("/admin add <player>"));
                return;
            }

            if (!sender.isConsoleSender())
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
                sender.send(tl("playerIsAdmin"));
                return;
            }

            plexPlayer.setRank(Rank.ADMIN.name());
            DataUtils.update(plexPlayer);
            Bukkit.getServer().getPluginManager().callEvent(new AdminAddEvent(sender, plexPlayer));
            return;
        }
        if (args[0].equalsIgnoreCase("remove"))
        {
            if (args.length != 2)
            {
                sender.send(usage("/admin remove <player>"));
                return;
            }

            if (!sender.isConsoleSender())
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
                sender.send(tl("playerNotAdmin"));
                return;
            }

            plexPlayer.setRank("");
            DataUtils.update(plexPlayer);
            Bukkit.getServer().getPluginManager().callEvent(new AdminRemoveEvent(sender, plexPlayer));
            return;
        }

        if (args[0].equalsIgnoreCase("setrank"))
        {
            if (args.length != 3)
            {
                sender.send(usage("/admin setrank <player> <rank>"));
                return;
            }

            if (!sender.isConsoleSender())
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
                sender.send(tl("rankNotFound"));
                return;
            }

            Rank rank = Rank.valueOf(args[2].toUpperCase());

            if (!rank.isAtLeast(Rank.ADMIN))
            {
                sender.send(tl("rankMustBeHigherThanAdmin"));
                return;
            }

            PlexPlayer plexPlayer = DataUtils.getPlayer(targetUUID);

            if (!isAdmin(plexPlayer))
            {
                sender.send(tl("playerNotAdmin"));
                return;
            }

            plexPlayer.setRank(rank.name().toLowerCase());
            DataUtils.update(plexPlayer);

            Bukkit.getServer().getPluginManager().callEvent(new AdminSetRankEvent(sender, plexPlayer, rank));

            return;
        }

        if (args[0].equalsIgnoreCase("list"))
        {
            if (args.length != 1)
            {
                sender.send(usage("/admin list"));
                return;
            }

            sender.send("Admins: " + StringUtils.join(plugin.getAdminList().getAllAdmins(), ", "));
            return;
        }
    }

    @Override
    public List<String> onTabComplete(CommandSource sender, String[] args)
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
