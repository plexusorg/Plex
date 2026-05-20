package dev.plex.command.impl;

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
        command.executes(context -> executeCommand(context));
        command.then(playerArgument("player")
                .executes(context -> executeCommand(context, string(context, "player"))));
    }

    @Override
    protected Component execute(@NotNull ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        Player playerSender = context.player();
        String[] args = context.args();
        if (args.length != 1)
        {
            return context.usage();
        }
        Player player = context.getNonNullPlayer(args[0]);
        PlexPlayer punishedPlayer = context.getOfflinePlexPlayer(player.getUniqueId());

        punishedPlayer.setLockedUp(!punishedPlayer.isLockedUp());
        if (punishedPlayer.isLockedUp())
        {
            player.openInventory(player.getInventory());
        }
        PlexUtils.broadcast(context.messageComponent(punishedPlayer.isLockedUp() ? "lockedUpPlayer" : "unlockedPlayer", sender.getName(), player.getName()));
        return null;
    }

}
