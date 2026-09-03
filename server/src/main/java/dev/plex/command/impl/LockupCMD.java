package dev.plex.command.impl;

import org.bukkit.Bukkit;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.player.PlexPlayer;
import dev.plex.util.PlexUtils;


import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class LockupCMD extends ServerCommand
{
    public LockupCMD()
    {
        super(command("lockup")
            .description("Lockup a player on the server")
            .usage("/<command> <player>")
            .permission("plex.lockup")
            .build());
    }
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context, ServerCommandContext::usage));
        command.then(playerArgument("player")
                .executes(context -> executeCommand(context, commandContext -> executeTyped(commandContext, string(context, "player")))));
    }

    private Component executeTyped(ServerCommandContext context, String playerName)
    {
        CommandSender sender = context.sender();
        Player playerSender = context.player();
        Player player = getNonNullPlayer(playerName);
        PlexPlayer punishedPlayer = getCachedPlexPlayer(player.getUniqueId());

        player.getScheduler().run(plugin, task ->
        {
            punishedPlayer.setLockedUp(!punishedPlayer.isLockedUp());
            if (punishedPlayer.isLockedUp()) player.openInventory(player.getInventory());
            PlexUtils.broadcast(PlexUtils.messageComponent(punishedPlayer.isLockedUp()
                    ? "lockedUpPlayer" : "unlockedPlayer", context.senderName(), player.getName()));
        }, null);
        return null;
    }

}
