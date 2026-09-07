package dev.plex.command.impl;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import org.bukkit.Bukkit;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.listener.impl.BlockListener;
import dev.plex.api.message.ActionBroadcast;
import dev.plex.util.CapturedActionBroadcast;
import dev.plex.util.PlexUtils;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.util.Objects;

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
        command.then(literal("list")
                .executes(context -> executeCommand(context, this::list)));
        command.then(literal("purge")
                .executes(context -> executeCommand(context, this::purge)));
        command.then(literal("all")
                .executes(context -> executeCommand(context, this::blockAll)));
        command.then(playerArgument("player")
                .executes(context -> executeCommand(context,
                        commandContext -> togglePlayer(commandContext, string(context, "player")))));
    }

    private Component list(ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        sender.sendMessage(PlexUtils.messageComponent("listOfPlayersBlocked"));
        for (String player : BlockListener.blockedPlayers)
        {
            sender.sendMessage(PlexUtils.messageComponent("blockeditListEntry", Placeholder.parsed("player", player)));
        }
        if (BlockListener.blockedPlayers.isEmpty())
        {
            sender.sendMessage(PlexUtils.messageComponent("blockeditListNone"));
        }
        return null;
    }

    private Component purge(ServerCommandContext context)
    {
        ActionBroadcast broadcast = CapturedActionBroadcast.capture(context.sender());
        broadcast.send(PlexUtils.messageComponent("unblockingEdits", Placeholder.parsed("sender", context.senderName()), Placeholder.parsed("players", PlexUtils.messageString("blockeditAllPlayers"))));
        int count = BlockListener.blockedPlayers.size();
        BlockListener.blockedPlayers.clear();
        return PlexUtils.messageComponent("blockeditSize", Placeholder.parsed("action", PlexUtils.messageString("blockeditUnblockedAction")), Placeholder.unparsed("count", String.valueOf(count)));
    }

    private Component blockAll(ServerCommandContext context)
    {
        ActionBroadcast broadcast = CapturedActionBroadcast.capture(context.sender());
        broadcast.send(PlexUtils.messageComponent("blockingEdits", Placeholder.parsed("sender", context.senderName()), Placeholder.parsed("players", PlexUtils.messageString("blockeditAllNonAdmins"))));
        long count = plugin.getPlayerService().cachedPlayers().stream()
                .map(player -> Bukkit.getPlayer(player.getUuid()))
                .filter(Objects::nonNull)
                .filter(player -> blockIfAllowed(context, player))
                .count();
        context.sender().sendMessage(PlexUtils.messageComponent("blockeditSize", Placeholder.parsed("action", PlexUtils.messageString("blockeditBlockedAction")), Placeholder.unparsed("count", String.valueOf(count))));
        return null;
    }

    private boolean blockIfAllowed(ServerCommandContext context, Player player)
    {
        boolean blocked = !context.silentCheckPermission(player, "plex.blockedit");
        if (blocked) BlockListener.blockedPlayers.add(player.getName());
        return blocked;
    }

    private Component togglePlayer(ServerCommandContext context, String playerName)
    {
        CommandSender sender = context.sender();
        final Player player = getNonNullPlayer(playerName);
        togglePlayer(context, sender, player);
        return null;
    }

    private void togglePlayer(ServerCommandContext context, CommandSender sender, Player player)
    {
        ActionBroadcast broadcast = CapturedActionBroadcast.capture(sender);
        if (!BlockListener.blockedPlayers.contains(player.getName()))
        {
            if (context.silentCheckPermission(player, "plex.blockedit"))
            {
                sender.sendMessage(PlexUtils.messageComponent("higherRankThanYou"));
                return;
            }
            broadcast.send(PlexUtils.messageComponent("blockingEdits", Placeholder.parsed("sender", context.senderName()), Placeholder.parsed("players", player.getName())));
            BlockListener.blockedPlayers.add(player.getName());
            player.sendMessage(PlexUtils.messageComponent("editsModified", Placeholder.parsed("state", PlexUtils.messageString("blockeditBlockedState"))));
            sender.sendMessage(PlexUtils.messageComponent("editsBlocked", Placeholder.parsed("player", player.getName())));
        }
        else
        {
            broadcast.send(PlexUtils.messageComponent("unblockingEdits", Placeholder.parsed("sender", context.senderName()), Placeholder.parsed("players", player.getName())));
            BlockListener.blockedPlayers.remove(player.getName());
            player.sendMessage(PlexUtils.messageComponent("editsModified", Placeholder.parsed("state", PlexUtils.messageString("blockeditUnblockedState"))));
            sender.sendMessage(PlexUtils.messageComponent("editsUnblocked", Placeholder.parsed("player", player.getName())));
        }
    }

}
