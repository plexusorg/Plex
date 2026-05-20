package dev.plex.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.ServerCommandContext;
import dev.plex.command.exception.CommandFailException;
import dev.plex.event.GameModeUpdateEvent;
import dev.plex.util.PlexUtils;


import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SpectatorCMD extends ServerCommand
{
    public SpectatorCMD()
    {
        super(command("spectator")
            .description("Set your own or another player's gamemode to spectator mode")
            .aliases("gmsp,egmsp,spec")
            .permission("plex.gamemode.spectator")
            .build());
    }
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context));
        command.then(word("target")
                .requires(source -> canUsePermission(source, "plex.gamemode.spectator.others"))
                .suggests(suggestPlayersAndAll("plex.gamemode.spectator.others"))
                .executes(context -> executeCommand(context, string(context, "target"))));
    }

    @Override
    protected Component execute(@NotNull ServerCommandContext context)
    {
        CommandSender sender = context.sender();
        Player playerSender = context.player();
        String[] args = context.args();
        if (args.length == 0)
        {
            if (context.isConsole(sender))
            {
                throw new CommandFailException(context.messageString("consoleMustDefinePlayer"));
            }
            Bukkit.getServer().getPluginManager().callEvent(new GameModeUpdateEvent(sender, playerSender, GameMode.SPECTATOR));
            return null;
        }

        if (context.checkPermission(sender, "plex.gamemode.spectator.others"))
        {
            if (args[0].equals("-a"))
            {
                for (Player targetPlayer : Bukkit.getServer().getOnlinePlayers())
                {
                    targetPlayer.setGameMode(GameMode.SPECTATOR);
                    context.messageComponent("gameModeSetTo", "spectator");
                }
                PlexUtils.broadcast(context.messageComponent("setEveryoneGameMode", sender.getName(), "spectator"));
                return null;
            }

            Player nPlayer = context.getNonNullPlayer(args[0]);
            Bukkit.getServer().getPluginManager().callEvent(new GameModeUpdateEvent(sender, nPlayer, GameMode.SPECTATOR));
        }
        return null;
    }

}
