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

public class SurvivalCMD extends ServerCommand
{
    public SurvivalCMD()
    {
        super(command("survival")
            .description("Set your own or another player's gamemode to survival mode")
            .aliases("gms,egms,esurvival,survivalmode,esurvivalmode")
            .permission("plex.gamemode.survival")
            .build());
    }
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context));
        command.then(word("target")
                .requires(source -> canUsePermission(source, "plex.gamemode.survival.others"))
                .suggests(suggestPlayersAndAll("plex.gamemode.survival.others"))
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
            Bukkit.getServer().getPluginManager().callEvent(new GameModeUpdateEvent(sender, playerSender, GameMode.SURVIVAL));
            return null;
        }

        if (context.checkPermission(sender, "plex.gamemode.survival.others"))
        {
            if (args[0].equals("-a"))
            {
                for (Player targetPlayer : Bukkit.getServer().getOnlinePlayers())
                {
                    targetPlayer.setGameMode(GameMode.SURVIVAL);
                    context.send(targetPlayer, context.messageComponent("gameModeSetTo", "survival"));
                }
                PlexUtils.broadcast(context.messageComponent("setEveryoneGameMode", sender.getName(), "survival"));
                return null;
            }

            Player nPlayer = context.getNonNullPlayer(args[0]);
            Bukkit.getServer().getPluginManager().callEvent(new GameModeUpdateEvent(sender, nPlayer, GameMode.SURVIVAL));
            return null;
        }
        return null;
    }

}
