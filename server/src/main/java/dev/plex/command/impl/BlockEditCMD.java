package dev.plex.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.listener.impl.BlockListener;
import dev.plex.util.PlexUtils;

import java.util.List;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class BlockEditCMD extends ServerCommand
{
    public BlockEditCMD()
    {
        super(command("blockedit")
            .description("Prevent players from modifying blocks")
            .usage("/<command> [list | purge | all | <player>]")
            .aliases("bedit")
            .permission("plex.blockedit")
            .build());
    }

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
    protected Component execute(@NotNull ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        Player playerSender = context.player();
        String[] args = context.args();
        if (args.length == 0)
        {
            return context.usage();
        }

        if (args[0].equalsIgnoreCase("list"))
        {
            context.send(sender, context.messageComponent("listOfPlayersBlocked"));

            int count = 0;
            for (String player : BlockListener.blockedPlayers.stream().toList())
            {
                context.send(sender, context.messageComponent("blockeditListEntry", player));
                ++count;
            }
            if (count == 0)
            {
                context.send(sender, context.messageComponent("blockeditListNone"));
            }
            return null;
        }
        else if (args[0].equalsIgnoreCase("purge"))
        {
            PlexUtils.broadcast(context.messageComponent("unblockingEdits", sender.getName(), context.messageString("blockeditAllPlayers")));
            int count = 0;
            for (String player : BlockListener.blockedPlayers.stream().toList())
            {
                if (BlockListener.blockedPlayers.contains(player))
                {
                    BlockListener.blockedPlayers.remove(player);
                    ++count;
                }
            }
            return context.messageComponent("blockeditSize", context.messageString("blockeditUnblockedAction"), count);
        }
        else if (args[0].equalsIgnoreCase("all"))
        {
            PlexUtils.broadcast(context.messageComponent("blockingEdits", sender.getName(), context.messageString("blockeditAllNonAdmins")));
            int count = 0;
            for (final Player player : Bukkit.getOnlinePlayers())
            {
                if (!context.silentCheckPermission(player, "plex.blockedit"))
                {
                    BlockListener.blockedPlayers.add(player.getName());
                    ++count;
                }
            }

            return context.messageComponent("blockeditSize", context.messageString("blockeditBlockedAction"), count);
        }

        final Player player = context.getNonNullPlayer(args[0]);
        if (!BlockListener.blockedPlayers.contains(player.getName()))
        {
            if (context.silentCheckPermission(player, "plex.blockedit"))
            {
                context.send(sender, context.messageComponent("higherRankThanYou"));
                return null;
            }
            PlexUtils.broadcast(context.messageComponent("blockingEdits", sender.getName(), player.getName()));
            BlockListener.blockedPlayers.add(player.getName());
            context.send(player, context.messageComponent("editsModified", context.messageString("blockeditBlockedState")));
            context.send(sender, context.messageComponent("editsBlocked", player.getName()));
        }
        else
        {
            PlexUtils.broadcast(context.messageComponent("unblockingEdits", sender.getName(), player.getName()));
            BlockListener.blockedPlayers.remove(player.getName());
            context.send(player, context.messageComponent("editsModified", context.messageString("blockeditUnblockedState")));
            context.send(sender, context.messageComponent("editsUnblocked", player.getName()));
        }
        return null;
    }

}
