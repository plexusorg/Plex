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

@CommandPermissions(permission = "plex.gamemode.adventure", source = RequiredCommandSource.ANY)
@CommandParameters(name = "adventure", aliases = "gma,egma,eadventure,adventuremode,eadventuremode", description = "Set your own or another player's gamemode to adventure mode")
public class AdventureCMD extends ServerCommand
{
    @Override
    protected void buildCommand(LiteralArgumentBuilder<CommandSourceStack> command)
    {
        command.executes(context -> executeCommand(context));
        command.then(word("target")
                .requires(source -> silentCheckPermission(source.getSender(), "plex.gamemode.adventure.others"))
                .suggests(suggestPlayersAndAll("plex.gamemode.adventure.others"))
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
            Bukkit.getServer().getPluginManager().callEvent(new GameModeUpdateEvent(sender, playerSender, GameMode.ADVENTURE));
            return null;
        }

        checkPermission(sender, "plex.gamemode.adventure.others");
        if (args[0].equals("-a"))
        {
            for (Player targetPlayer : Bukkit.getServer().getOnlinePlayers())
            {
                targetPlayer.setGameMode(GameMode.ADVENTURE);
                messageComponent("gameModeSetTo", "adventure");
            }
            PlexUtils.broadcast(messageComponent("setEveryoneGameMode", sender.getName(), "adventure"));
            return null;
        }

        Player nPlayer = getNonNullPlayer(args[0]);
        Bukkit.getServer().getPluginManager().callEvent(new GameModeUpdateEvent(sender, nPlayer, GameMode.ADVENTURE));
        return null;
    }

}
