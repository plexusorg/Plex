package dev.plex.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.listener.impl.BlockListener;
import dev.plex.util.PlexUtils;

import java.util.List;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandPermissions(permission = "plex.blockedit")
@CommandParameters(name = "blockedit", usage = "/<command> [list | purge | all | <player>]", aliases = "bedit", description = "Prevent players from modifying blocks")
public class BlockEditCMD extends ServerCommand
{
    private final BlockListener bl = new BlockListener();

    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context));
        command.then(literal("list")
                .executes(context -> executeCommand(context, "list")));
        command.then(literal("purge")
                .executes(context -> executeCommand(context, "purge")));
        command.then(literal("all")
                .executes(context -> executeCommand(context, "all")));
        command.then(playerArgument("player")
                .executes(context -> executeCommand(context, string(context, "player"))));
    }

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
                send(sender, messageComponent("blockeditListEntry", player));
                ++count;
            }
            if (count == 0)
            {
                send(sender, messageComponent("blockeditListNone"));
            }
            return null;
        }
        else if (args[0].equalsIgnoreCase("purge"))
        {
            PlexUtils.broadcast(messageComponent("unblockingEdits", sender.getName(), messageString("blockeditAllPlayers")));
            int count = 0;
            for (String player : bl.blockedPlayers.stream().toList())
            {
                if (bl.blockedPlayers.contains(player))
                {
                    bl.blockedPlayers.remove(player);
                    ++count;
                }
            }
            return messageComponent("blockeditSize", messageString("blockeditUnblockedAction"), count);
        }
        else if (args[0].equalsIgnoreCase("all"))
        {
            PlexUtils.broadcast(messageComponent("blockingEdits", sender.getName(), messageString("blockeditAllNonAdmins")));
            int count = 0;
            for (final Player player : Bukkit.getOnlinePlayers())
            {
                if (!silentCheckPermission(player, "plex.blockedit"))
                {
                    bl.blockedPlayers.add(player.getName());
                    ++count;
                }
            }

            return messageComponent("blockeditSize", messageString("blockeditBlockedAction"), count);
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
            send(player, messageComponent("editsModified", messageString("blockeditBlockedState")));
            send(sender, messageComponent("editsBlocked", player.getName()));
        }
        else
        {
            PlexUtils.broadcast(messageComponent("unblockingEdits", sender.getName(), player.getName()));
            bl.blockedPlayers.remove(player.getName());
            send(player, messageComponent("editsModified", messageString("blockeditUnblockedState")));
            send(sender, messageComponent("editsUnblocked", player.getName()));
        }
        return null;
    }

}
