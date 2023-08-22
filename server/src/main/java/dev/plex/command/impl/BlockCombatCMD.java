package dev.plex.command.impl;

import dev.plex.command.PlexCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.listener.impl.CombatListener;
import dev.plex.rank.enums.Rank;
import dev.plex.util.PlexUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandPermissions(level = Rank.ADMIN, permission = "plex.blockpvp")
@CommandParameters(name = "blockpvp", usage = "/<command> [list | purge | all | <player>]", aliases = "bpvp", description = "Prevent players from engaging PvP")
public class BlockCombatCMD extends PlexCommand
{
    private final CombatListener cl = new CombatListener();

    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, @NotNull String[] args)
    {
        if (args.length == 0)
        {
            return usage();
        }

        if (args[0].equalsIgnoreCase("list"))
        {
            send(sender, messageComponent("listOfPlayersCombatBlocked"));

            int count = 0;
            for (String player : cl.blockedPlayers.stream().toList())
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
            PlexUtils.broadcast(messageComponent("unblockingCombat", sender.getName(), "all players"));
            int count = 0;
            for (String player : cl.blockedPlayers.stream().toList())
            {
                if (cl.blockedPlayers.contains(player))
                {
                    cl.blockedPlayers.remove(player);
                    ++count;
                }
            }
            return messageComponent("combatSize", "Unblocked", count);
        }
        else if (args[0].equalsIgnoreCase("all"))
        {
            PlexUtils.broadcast(messageComponent("blockingCombat", sender.getName(), "all non-admins"));
            int count = 0;
            for (final Player player : Bukkit.getOnlinePlayers())
            {
                if (!silentCheckRank(player, Rank.ADMIN, "plex.blockpvp"))
                {
                    cl.blockedPlayers.add(player.getName());
                    ++count;
                }
            }
            return messageComponent("combatSize", "Blocked", count);
        }

        final Player player = getNonNullPlayer(args[0]);
        if (!cl.blockedPlayers.contains(player.getName()))
        {
            if (silentCheckRank(player, Rank.ADMIN, "plex.blockpvp"))
            {
                send(sender, messageComponent("higherRankThanYou"));
                return null;
            }
            PlexUtils.broadcast(messageComponent("blockingCombat", sender.getName(), player.getName()));
            cl.blockedPlayers.add(player.getName());
            send(player, messageComponent("combatModified", "blocked"));
            send(sender, messageComponent("combatUnblocked", player.getName()));
        }
        else
        {
            PlexUtils.broadcast(messageComponent("unblockingCombat", sender.getName(), player.getName()));
            cl.blockedPlayers.remove(player.getName());
            send(player, messageComponent("combatModified", "unblocked"));
            send(sender, messageComponent("combatUnblocked", player.getName()));
        }
        return null;
    }
}
