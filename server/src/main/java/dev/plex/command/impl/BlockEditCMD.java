package dev.plex.command.impl;

import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.listener.impl.BlockListener;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@CommandPermissions(permission = "plex.blockedit")
@CommandParameters(name = "blockedit", usage = "/<command> [list | purge | all | <player>]", aliases = "bedit", description = "Prevent players from modifying blocks")
public class BlockEditCMD extends PlexCommand
{
    private final BlockListener bl = new BlockListener();

    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, @NotNull String[] args)
    {
        if (args.length == 0)
        {
            return usage();
        }

        if (args[0].equalsIgnoreCase("list"))
        {
            send(sender, messageComponent("listOfPlayersBlocked"));

            int count = 0;
            for (String player : bl.blockedPlayers.stream().toList())
            {
                send(sender, "- " + player);
                ++count;
            }
            if (count == 0)
            {
                send(sender, "- none");
            }
            return null;
        }
        else if (args[0].equalsIgnoreCase("purge"))
        {
            PlexUtils.broadcast(messageComponent("unblockingEdits", sender.getName(), "all players"));
            int count = 0;
            for (String player : bl.blockedPlayers.stream().toList())
            {
                if (bl.blockedPlayers.contains(player))
                {
                    bl.blockedPlayers.remove(player);
                    ++count;
                }
            }
            return messageComponent("blockeditSize", "Unblocked", count);
        }
        else if (args[0].equalsIgnoreCase("all"))
        {
            PlexUtils.broadcast(messageComponent("blockingEdits", sender.getName(), "all non-admins"));
            int count = 0;
            for (final Player player : Bukkit.getOnlinePlayers())
            {
                if (!silentCheckPermission(player, "plex.blockedit"))
                {
                    bl.blockedPlayers.add(player.getName());
                    ++count;
                }
            }

            return messageComponent("blockeditSize", "Blocked", count);
        }

        final Player player = getNonNullPlayer(args[0]);
        if (!bl.blockedPlayers.contains(player.getName()))
        {
            if (silentCheckPermission(player, "plex.blockedit"))
            {
                send(sender, messageComponent("higherRankThanYou"));
                return null;
            }
            PlexUtils.broadcast(messageComponent("blockingEdits", sender.getName(), player.getName()));
            bl.blockedPlayers.add(player.getName());
            send(player, messageComponent("editsModified", "blocked"));
            send(sender, messageComponent("editsBlocked", player.getName()));
        }
        else
        {
            PlexUtils.broadcast(messageComponent("unblockingEdits", sender.getName(), player.getName()));
            bl.blockedPlayers.remove(player.getName());
            send(player, messageComponent("editsModified", "unblocked"));
            send(sender, messageComponent("editsUnblocked", player.getName()));
        }
        return null;
    }

    @Override
    public @NotNull List<String> smartTabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException
    {
        if (silentCheckPermission(sender, this.getPermission()))
        {
            List<String> options = new ArrayList<>();
            if (args.length == 1)
            {
                options.addAll(Arrays.asList("list", "purge", "all"));
                options.addAll(PlexUtils.getPlayerNameList());
                return options;
            }
        }
        return Collections.emptyList();
    }
}
