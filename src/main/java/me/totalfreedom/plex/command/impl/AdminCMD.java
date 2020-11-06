package me.totalfreedom.plex.command.impl;

import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import me.totalfreedom.plex.cache.DataUtils;
import me.totalfreedom.plex.command.PlexCommand;
import me.totalfreedom.plex.command.annotation.CommandParameters;
import me.totalfreedom.plex.command.annotation.CommandPermissions;
import me.totalfreedom.plex.command.exception.PlayerNotFoundException;
import me.totalfreedom.plex.command.source.CommandSource;
import me.totalfreedom.plex.command.source.RequiredCommandSource;
import me.totalfreedom.plex.event.AdminAddEvent;
import me.totalfreedom.plex.event.AdminRemoveEvent;
import me.totalfreedom.plex.event.AdminSetRankEvent;
import me.totalfreedom.plex.player.PlexPlayer;
import me.totalfreedom.plex.rank.enums.Rank;
import me.totalfreedom.plex.util.PlexUtils;
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
                sender.send("Console only");
                return;
            }

            UUID targetUUID = PlexUtils.getFromName(args[1]);

            if (targetUUID == null || !DataUtils.hasPlayedBefore(targetUUID))
            {
                throw new PlayerNotFoundException();
            }
            PlexPlayer plexPlayer = DataUtils.getPlayer(targetUUID);

            if (isAdmin(plexPlayer))
            {
                sender.send("Player is an admin");
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
                sender.send("Console only");
                return;
            }

            UUID targetUUID = PlexUtils.getFromName(args[1]);

            if (targetUUID == null || !DataUtils.hasPlayedBefore(targetUUID))
            {
                throw new PlayerNotFoundException();
            }
            PlexPlayer plexPlayer = DataUtils.getPlayer(targetUUID);

            if (!isAdmin(plexPlayer))
            {
                sender.send("Player is not an admin");
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
                sender.send("Console only");
                return;
            }

            UUID targetUUID = PlexUtils.getFromName(args[1]);

            if (targetUUID == null || !DataUtils.hasPlayedBefore(targetUUID))
            {
                throw new PlayerNotFoundException();
            }

            if (!rankExists(args[2]))
            {
                sender.send("Rank not found");
                return;
            }

            Rank rank = Rank.valueOf(args[2].toUpperCase());

            if (!rank.isAtLeast(Rank.ADMIN))
            {
                sender.send("Must be admin+");
                return;
            }

            PlexPlayer plexPlayer = DataUtils.getPlayer(targetUUID);

            if (!isAdmin(plexPlayer))
            {
                sender.send("Player is not an admin");
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
