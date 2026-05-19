package dev.plex.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.plex.command.ServerCommand;
import dev.plex.command.annotation.CommandParameters;
import dev.plex.command.annotation.CommandPermissions;
import dev.plex.command.exception.CommandFailException;
import dev.plex.command.source.RequiredCommandSource;
import dev.plex.event.GameModeUpdateEvent;
import dev.plex.util.PlexUtils;


import io.papermc.paper.command.brigadier.CommandSourceStack;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@CommandPermissions(permission = "plex.gamemode.survival", source = RequiredCommandSource.ANY)
@CommandParameters(name = "survival", aliases = "gms,egms,esurvival,survivalmode,esurvivalmode", description = "Set your own or another player's gamemode to survival mode")
public class SurvivalCMD extends ServerCommand
{
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context));
        command.then(word("target")
                .requires(source -> silentCheckPermission(source.getSender(), "plex.gamemode.survival.others"))
                .suggests(suggestPlayersAndAll("plex.gamemode.survival.others"))
                .executes(context -> executeCommand(context, string(context, "target"))));
    }

    @Override
    protected Component execute(@NotNull CommandSender sender, @Nullable Player playerSender, String[] args)
    {
        if (args.length == 0)
        {
            if (isConsole(sender))
            {
                throw new CommandFailException(messageString("consoleMustDefinePlayer"));
            }
            Bukkit.getServer().getPluginManager().callEvent(new GameModeUpdateEvent(sender, playerSender, GameMode.SURVIVAL));
            return null;
        }

        if (checkPermission(sender, "plex.gamemode.survival.others"))
        {
            if (args[0].equals("-a"))
            {
                for (Player targetPlayer : Bukkit.getServer().getOnlinePlayers())
                {
                    targetPlayer.setGameMode(GameMode.SURVIVAL);
                    send(targetPlayer, messageComponent("gameModeSetTo", "survival"));
                }
                PlexUtils.broadcast(messageComponent("setEveryoneGameMode", sender.getName(), "survival"));
                return null;
            }

            Player nPlayer = getNonNullPlayer(args[0]);
            Bukkit.getServer().getPluginManager().callEvent(new GameModeUpdateEvent(sender, nPlayer, GameMode.SURVIVAL));
            return null;
        }
        return null;
    }

}
